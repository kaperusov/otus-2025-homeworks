import requests
from flask import request, jsonify

from app.config import ConfigReader
from app.config_store import ConfigStore
from app.logger_store import LoggerStore


class BackendClient:

    _routes = None  # Кэшируем маршруты

    @classmethod
    def initialize_routes(cls, config):
        cls._routes = []
        route_config = config.get_route_config()

        for service, params in route_config.items():
            cls._routes.append({
                'prefix': params['prefix'],
                'url': params['url'],
                'service': service
            })

        # Сортируем по длине префикса (для более специфичных маршрутов)
        cls._routes.sort(key=lambda x: len(x['prefix']), reverse=True)

    @staticmethod
    def proxy_request(path):

        # Определяем целевой сервис на основе пути
        if not BackendClient._routes:
            config = ConfigStore.get_config()
            BackendClient.initialize_routes(config)

        # Ищем подходящий маршрут
        backend_url = None
        service_path = path

        for route in BackendClient._routes:
            if path.startswith(route['prefix']):
                backend_url = route['url']
                service_path = path #path[len(route['prefix']):]
                break

        # Если маршрут не найден - используем дефолтный URL
        if not backend_url:
            config = ConfigStore.get_config()
            backend_url = config.get_value(ConfigReader.GENERAL, 'default_service_url')

        headers = {
            'Authorization': request.headers.get('Authorization'),
            'Content-Type': request.headers.get('Content-Type')
        }
        # Удаляем None значения из headers
        headers = {k: v for k, v in headers.items() if v is not None}

        url = f"{backend_url.rstrip('/')}/{service_path.lstrip('/')}"

        try:
            response = requests.request(
                method=request.method,
                url=url,
                headers=headers,
                data=request.get_data(),
                params=request.args,
                cookies=request.cookies,
                allow_redirects=False,
                timeout=30  # Добавляем таймаут
            )

            # Создаем исключение для кодов 4xx/5xx
            response.raise_for_status()

            # Фильтруем hop-by-hop заголовки, которые относятся к транспортному уровню и
            # и могут нарушить работу прокси и балансировщиков, поэтому запрещены спецификацией WSGI (PEP 3333)
            excluded_headers = [
                'connection', 'keep-alive', 'proxy-authenticate',
                'proxy-authorization', 'te', 'trailers', 'transfer-encoding',
                'upgrade'
            ]
            response_headers = [
                (name, value) for (name, value) in response.headers.items()
                if name.lower() not in excluded_headers
            ]

            return response.content, response.status_code, response_headers

        except requests.exceptions.RequestException as e:
            logger = LoggerStore.get_logger()
            logger.error(f"Backend request failed: {str(e)}")
            error_response = getattr(e.response, 'json', lambda: {})() or {}
            return jsonify(error_response), getattr(e.response, 'status_code', 500)

from flask import current_app
from keycloak import KeycloakOpenID, KeycloakAdmin
import requests
from requests import RequestException

from app.config_store import ConfigStore
from app.logger_store import LoggerStore


class KeycloakService:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            config = ConfigStore.get_config()
            keycloak_cfg = config.get_section(config.KEYCLOAK)

            cls._instance.server_url = keycloak_cfg['url']
            cls._instance.client_id = keycloak_cfg['client_id']
            cls._instance.realm_name = keycloak_cfg['realm']
            cls._instance.client_secret_key = keycloak_cfg['client_secret']

            cls.logger = LoggerStore.get_logger()

            cls.keycloak_openid = KeycloakOpenID(
                server_url=cls._instance.server_url,
                client_id=cls._instance.client_id,
                realm_name=cls._instance.realm_name,
                client_secret_key=cls._instance.client_secret_key,
            )

            cls.keycloak_admin = KeycloakAdmin(
                server_url=cls._instance.server_url,
                username=keycloak_cfg['admin_username'],
                password=keycloak_cfg['admin_password'],
            )
        return cls._instance

    def authenticate(self, username, password):
        try:
            token = self.keycloak_openid.token(username, password)
            return token
        except Exception as e:
            current_app.logger.error(f"Auth error: {str(e)}")
            return None

    def get_user(self, username: str):
        user_id = self.keycloak_admin.get_user_id(username)
        return self.get_user_by_id(user_id)

    def get_user_by_id(self, user_id: str):
        return self.keycloak_admin.get_user(user_id)

    def validate_keycloak_token(self, token):
        """Проверяет токен через Keycloak"""
        try:
            return self.keycloak_openid.decode_token(
                token,
                self.keycloak_openid.public_key()
            )
        except Exception as e:
            raise ValueError(f"Keycloak token validation failed: {str(e)}")

    def register(self, user_data):
        # Add user and set password
        new_user = self.keycloak_admin.create_user(user_data)
        return new_user


    def update_user(self, user_id: str, payload):
        # Add user and set password
        new_user = self.keycloak_admin.update_user(user_id=user_id, payload=payload)
        return new_user

    def check_keycloak_service(self):
        """Проверка доступности Keycloak через его health-эндпоинт"""
        try:
            if not self.server_url:
                return False

            # Формируем URL для проверки health (стандартный эндпоинт Keycloak)
            health_url = f"{self.server_url.rstrip('/')}/health"

            # Отправляем GET-запрос с таймаутом (например, 3 секунды)
            response = requests.get(
                health_url,
                timeout=3,
                headers={'Accept': 'application/json'}
            )

            # Проверяем статус ответа и содержимое (Keycloak возвращает 200 и "status": "UP")
            if response.status_code == 200:
                health_data = response.json()
                return health_data.get('status') == 'UP'

            self.logger.warning(f"Keycloak health check failed: {health_url} -> {response.status_code}")
            return False

        except RequestException as e:
            self.logger.error(f"Keycloak health check failed: {str(e)}")
            return False
        except ValueError as e:
            self.logger.error(f"Invalid JSON response from Keycloak: {str(e)}")
            return False

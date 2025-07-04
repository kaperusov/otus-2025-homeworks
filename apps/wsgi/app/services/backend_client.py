import requests
from flask import request

from app.config import ConfigReader
from app.config_store import ConfigStore


class BackendClient:

    @staticmethod
    def proxy_request(path):
        config = ConfigStore.get_config()
        backend_url = config.get_value(ConfigReader.GENERAL, 'backend_url')
        headers = {
            'Authorization': request.headers.get('Authorization')
        }

        response = requests.request(
            method=request.method,
            url=f"{backend_url}/{path}",
            headers=headers,
            data=request.get_data(),
            params=request.args,
            cookies=request.cookies,
            allow_redirects=False
        )

        return response.content, response.status_code, response.headers.items()
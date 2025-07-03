from flask import Blueprint
from app.services.backend_client import BackendClient

api_bp = Blueprint('api', __name__)

@api_bp.route('/<path:path>', methods=['GET', 'POST', 'PUT', 'DELETE'])
def proxy_to_backend(path):
    return BackendClient.proxy_request(path)
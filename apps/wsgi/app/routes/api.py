from flask import Blueprint, jsonify
from app.services.backend_client import BackendClient
from app.logger_store import LoggerStore

api_bp = Blueprint('api', __name__)

@api_bp.route('/<path:path>', methods=['GET', 'POST', 'PUT', 'DELETE'])
def proxy_to_backend(path):
    try:
        return BackendClient.proxy_request(path)
    except Exception as e:
        logger = LoggerStore.get_logger()
        logger.error(f"Proxy error: {str(e)}")
        return jsonify({"error": "Internal Server Error"}), 500
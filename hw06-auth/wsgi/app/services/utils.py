from functools import wraps
from flask import request, jsonify

from app.services.keycloak_service import KeycloakService

def jwt_required(f):
    """Декоратор для проверки JWT"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization')
        if not auth_header:
            return jsonify({"error": "Authorization header is missing"}), 401

        try:
            kc = KeycloakService()
            token = auth_header.split()[1]  # Bearer <token>
            payload = kc.validate_keycloak_token(token)
            request.user = payload  # Добавляем payload в request
        except Exception as e:
            return jsonify({"error": str(e)}), 401

        return f(*args, **kwargs)
    return decorated_function
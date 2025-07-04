from flask import Blueprint, jsonify

from app.routes.utils import make_response
from app.services.keycloak_service import KeycloakService

state_bp = Blueprint('state', __name__)

@state_bp.route('/health', methods=['GET'])
def health_check():
    """Проверка состояния приложения"""
    return jsonify({
        "status": "OK",
        "message": "Application is healthy"
    }), 200


@state_bp.route('/ready', methods=['GET'])
def readiness_check():
    """Проверка готовности приложения к работе"""
    try:
        kc = KeycloakService()
        if kc.check_keycloak_service():
            return make_response(200,
                                 message="Application is ready to handle requests",
                                 dependencies={
                                     "keycloak_service": "available"
                                 })
        else:
            return make_response(503,
                                 message="Application is not ready",
                                 dependencies={
                                             "keycloak_service": "unavailable"
                                 })
    except Exception as e:
        return make_response(500,
                             message=f"Readiness check failed: {str(e)}")



from flask import Blueprint, jsonify

from app.routes.utils import make_response

state_bp = Blueprint('state', __name__)

@state_bp.route('/health', methods=['GET'])
def health_check():
    """Проверка состояния приложения"""
    return make_response(200,
                         message="Application is healthy")


@state_bp.route('/ready', methods=['GET'])
def readiness_check():
    """Проверка готовности приложения к работе"""
    try:
        return make_response(200,
                             message="Application is ready to handle requests")
    except Exception as e:
        return make_response(500,
                             message=f"Readiness check failed: {str(e)}")



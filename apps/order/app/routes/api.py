from flask import Blueprint, jsonify

from app.routes.utils import make_response

api_bp = Blueprint('api', __name__)


@api_bp.route('/pay', methods=['POST'])
def pay():
    """Проверка состояния приложения"""
    return make_response(400,
                     message="You don't have money")
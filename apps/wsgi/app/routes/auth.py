import json

from flask import Blueprint, request, jsonify, Response
from keycloak import KeycloakPostError

from app.routes.utils import make_response
from app.services.keycloak_service import KeycloakService
from app.services.session_manager import SessionManager
from app.services.utils import jwt_required

auth_bp = Blueprint('auth', __name__)
kc = KeycloakService()

@auth_bp.route('/register', methods=['POST'])
def register():
    try:
        user_data = {
            "email": __get_request_form_value('email', True),
            "username": __get_request_form_value('username', True),
            "enabled": True,
            "firstName": __get_request_form_value('firstName'),
            "lastName": __get_request_form_value('lastName'),
            "credentials": [
                {
                    "value": __get_request_form_value('password', True),
                    "type": "password",
                }
            ]
        }
    except Exception as e:
        return make_response(400,
                             message=f"Invalid user data: {str(e)}")

    try:
        user_id = kc.register(user_data)
        user = kc.get_user_by_id(user_id)
        return make_response(201, "User created", user=user)
    except KeycloakPostError as e:
        return make_response(code=e.response_code,
                             message="Failed to save user data",
                             keycloak_error_message=json.loads(e.error_message))
    except Exception as e:
        return make_response(code=500, message=f"Failed to save user data. Unknown error: {str(e)}")


@auth_bp.route('/profile/<user_id>', methods=['PUT'])
@jwt_required
def update_profile(user_id):
    has_access, bad_response = __check_access(user_id)
    if not has_access:
        return bad_response
    try:
        payload = dict(
            (key, value)
            for key, value in [
                ("email", __get_request_form_value('email')),
                ("firstName", __get_request_form_value('firstName')),
                ("lastName", __get_request_form_value('lastName')),
            ]
            if value is not None and value != ""
        )
    except Exception as e:
        return make_response(400,
                             message=f"Invalid user data: {str(e)}")

    try:
        kc.update_user(user_id, payload)
        updated_user_data = kc.get_user_by_id(user_id)
        username = updated_user_data['username']
        return make_response(code=200,
                             message=f"User {username} ({user_id}) has been updated successfully.",
                             user=updated_user_data)
    except KeycloakPostError as e:
        return make_response(code=e.response_code,
                             message="Failed to update user data",
                             keycloak_error_message=json.loads(e.error_message))
    except Exception as e:
        return make_response(code=500, message=f"Failed to update user data. Unknown error: {str(e)}")


@auth_bp.route('/profile/me', methods=['GET'])
@jwt_required
def get_profile_current_user():
    if hasattr(request, 'user'):
        user_data = request.user
        user = kc.get_user(user_data['preferred_username'])
        return jsonify(user), 200

    return make_response(500, "User data in request is empty... ")


@auth_bp.route('/profile/<user_id>', methods=['GET'])
@jwt_required
def get_profile_by_id(user_id: str):
    has_access, bad_response = __check_access(user_id)
    if not has_access:
        return bad_response
    # assert user_id == request.view_args['user_id']
    # has_access = False
    # if hasattr(request, 'user'):
    #     user_data = request.user
    #     has_access = user_data['sub'] == user_id
    #
    # if not has_access:
    #     return make_response(403, "Access denied")

    user = kc.get_user_by_id(user_id)
    if user:
        return jsonify(user), 200

    return make_response(404, "User not found")


@auth_bp.route('/', methods=['POST'])
def auth():
    """
    Авторизация
    :return: данные авторизованного пользователя
    """
    username = request.form['username']
    password = request.form['password']

    session_manager = SessionManager()
    session = session_manager.get_session_by_username(username)
    if session:
        return jsonify(session), 200
    else:
        authenticate = kc.authenticate(username, password)
        if authenticate:
            # Создаем сессию
            session_manager.create_session(username, authenticate['access_token'])

            return jsonify(authenticate), 200
        else:
            return make_response(401, "Wrong username or password")


def __check_access(user_id) -> tuple[bool, tuple[Response, int]] | tuple[bool, None]:
    assert user_id == request.view_args['user_id']
    has_access = False
    if hasattr(request, 'user'):
        user_data = request.user
        has_access = user_data['sub'] == user_id

    if has_access:
        return True, None
    else:
        return False, make_response(403, "Access denied")


def __get_request_form_value(key: str, required: bool = False, default_value: str = None):
    """
    Извлечение переменных из запроса с проверкой на обязательность
    :param key: наименование поля
    :param required: флаг обязательности
    :param default_value: значение по умолчанию, работает только для необязательных полей
    :return: извлеченное значение в виде строки
    """
    if key in request.form:
        return request.form[key]
    else:
        if required:
            raise ValueError(f"{key} field is required")
        else:
            return default_value
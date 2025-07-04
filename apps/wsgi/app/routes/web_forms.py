from flask import Blueprint, request, redirect, render_template

from app.config_store import ConfigStore
from app.routes.utils import make_response
from app.services.keycloak_service import KeycloakService
from app.services.session_manager import SessionManager

web_bp = Blueprint('web_form', __name__)

@web_bp.route('/', methods=['GET'])
def home():
    access_token = get_current_access_token()
    if access_token:
        redirect_url = ConfigStore.get_config(section='GENERAL')['redirect_url']
    else:
        redirect_url = "/login"

    return redirect(redirect_url)


@web_bp.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']

        kc = KeycloakService()
        authenticate = kc.authenticate(username, password)

        if authenticate:
            # Создаем сессию
            session_manager = SessionManager()
            session_id = session_manager.create_session(username, authenticate['access_token'])

            redirect_url = ConfigStore.get_config(section='GENERAL')['redirect_url']
            response = redirect(redirect_url)
            response.set_cookie('session_id', session_id)

            if 'rememberMe' in request.form and request.form['rememberMe']:
                response.set_cookie('username', username)
                response.set_cookie('password', password)
                response.set_cookie('rememberMe', "True")
            else:
                response.delete_cookie('username')
                response.delete_cookie('password')
                response.delete_cookie('rememberMe')
            return response

        return render_template('login.html', error="Неверные логин или пароль.")

    # ==> GET
    return render_template('login.html',
                           username=get_cookies('username'),
                           password=get_cookies('password'),
                           rememberMe=get_cookies('rememberMe'))


@web_bp.route('/logout', methods=['GET', 'POST'])
def logout():
    if request.method == 'GET':
        response = redirect('/login')
        response.delete_cookie('access_token')
        return response
    else:
        if hasattr(request, 'user'):
            user_data = request.user
            kc = KeycloakService()
            username = kc.get_user(user_data['preferred_username'])
            return make_response(200, f"The user {username} has successfully logged out.")

        return make_response(200, "The user has successfully logged out.")


@web_bp.route('/token', methods=['GET'])
def token():
    session_manager = SessionManager()
    session_id = get_cookies('session_id')
    session = session_manager.get_session(session_id) if session_id else None
    access_token = session['access_token'] if session else None
    if access_token is None or len(access_token) == 0:
        return redirect('/login', code=401)

    return render_template('token.html',
                           jwt_token=access_token,
                           username=session['username'],
                           created_at=session['created_at'],
                           expires_at=session['expires_at'],
                           session_id=session_id
                           )


def get_current_session():
    session_manager = SessionManager()
    session_id = get_cookies('session_id')
    return session_manager.get_session(session_id) if session_id else None

def get_current_access_token():
    session = get_current_session()
    return session['access_token'] if session else None

def get_current_username():
    session = get_current_session()
    return session['username'] if session else None

def get_cookies(key: str):
    return request.cookies[key] if key in request.cookies else ''


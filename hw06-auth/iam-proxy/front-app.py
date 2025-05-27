#!./venv/bin/python3
from flask import Flask, request, Response, render_template, redirect, make_response, jsonify
import os
import argparse
from waitress import serve
from pathlib import Path
import logging
from configparser import ConfigParser
import requests
from requests.exceptions import RequestException
import time
import datetime
import secrets
import jwt

# Argument parser
parser = argparse.ArgumentParser()
parser.add_argument("-p", "--profile",
                    help="""
                    Application profile name. 
                    The configuration file will be selected by the profile name, 
                    using the template: config-{profile}.ini
                    """,
                    type=str)
args = parser.parse_args()

# Config parser
cfg = ConfigParser(interpolation=None)

# Logger
logger = logging.getLogger("front-app")

if not Path("config.ini").exists():
    print( "Configuration files not found. Exit with error 1")
    exit( 1 )

APP_NAME = 'front-app'
APP_VERSION = None

config_filenames = ["config.ini"]
if args.profile:
    ACTIVE_PROFILE = args.profile
    profile_config = f"config-{ACTIVE_PROFILE}.ini"
    if Path(profile_config).exists():
        config_filenames.append(profile_config)
else:
    ACTIVE_PROFILE = 'default'

cfg.read(config_filenames, encoding='utf-8')

# Global variables
GENERAL            = cfg['general']
REDIRECT_URL       = GENERAL.get('redirect_url')
DEBUG_MODE         = GENERAL.getboolean('debug_mode', False)
LAST_REQUESTED_URL = None


# Routing
_ROOT   = '/'
_AUTH   = '/auth/'
_LOGIN  = '/login/'
_LOGOUT_BASE_PATH = '/logout/'
_LOGOUT_ADDITIONAL = GENERAL.get('additional_logout_path', '/signout/')
_TOKEN  = '/token'
_404    = '/not_found'
_500    = '/error'



# ----------------------------------------------------------------------------------------------------------------------
class Session:
    id: None
    username: None
    password: None
    wrong_password = False
    token: None
    refresh_token: None
# ---------------------------------
SESSIONS = {}
SESSION_ID_KEY = 'PLATFORM_SESSION'
# ----------------------------------------------------------------------------------------------------------------------
flask = Flask(__name__)

def main():
    global APP_NAME
    global APP_VERSION
    host = GENERAL.get('host', "0.0.0.0")
    port = GENERAL.getint('port', 5000)

    with open('.version') as f:
        APP_VERSION = f.readline().strip('\n')

    if DEBUG_MODE:
        flask.run(host=host, port=port, debug=True )
    else:
        init_log()
        logger.info( f"-----------------{APP_NAME}_v{APP_VERSION}-----------------" )
        logger.info( f"Active profile: {ACTIVE_PROFILE}")
        serve(flask, host=host, port=port)
        logger.info( "Flask server on finished. Bye.")
        logger.info( "***" )

# ---------------------------------------




def get_current_session():
    session_id = request.cookies.get(SESSION_ID_KEY)
    if session_id is None:
        session_id = request.args.get(SESSION_ID_KEY)
    return get_session(session_id)


def get_session(session_id):
    global SESSIONS
    if session_id is not None:
        return SESSIONS.get(session_id)
    else:
        return None


@flask.route('/', defaults={'path': ''}, methods=['GET', 'POST', 'PUT', 'DELETE', 'PATCH'])
@flask.route('/<path:path>', methods=['GET', 'POST', 'PUT', 'DELETE', 'PATCH'])
def proxy_ui(path):
    # Construct the full URL
    url = f"{GENERAL.get('ui_url')}/{path}"
    logger.debug(f"Route to UI: -> {url}")
    return build_response(url=url, path=path)


@flask.route('/api/<path:path>', methods=['GET', 'POST', 'PUT', 'DELETE', 'PATCH'])
def proxy_api(path):
    # Construct the full URL
    url = f"{GENERAL.get('api_url')}/api/{path}"
    logger.debug(f"Route to API: -> {url}")
    return build_response(url=url, path=path)


@flask.route('/swagger-ui/<path:path>', methods=['GET', 'POST', 'PUT', 'DELETE', 'PATCH'])
def proxy_swagger(path):
    # Construct the full URL
    url = f"{GENERAL.get('api_url')}/swagger-ui/{path}"
    logger.debug(f"Route to SWAGGER: ->> {url}")
    return build_response(url=url, path=path)


def build_response(url, path: str):
    global LAST_REQUESTED_URL

    try:
        s = get_current_session()
        if s is None or s.token is None:
            excluding_paths = ['', _ROOT, _LOGIN, _TOKEN, '/favicon.ico']
            if LAST_REQUESTED_URL is None and path is not None and path not in excluding_paths:
                LAST_REQUESTED_URL = f"/{path}"
                logger.debug(f"Save LAST_REQUESTED_URL = {LAST_REQUESTED_URL}")
            return redirect(_LOGIN, code=302)

        if not token_is_fresh(s.token):
            new_token, new_refresh_token = refresh_keycloak_token(s.refresh_token)
            if new_token:
                s.token = new_token
                s.refresh_token = new_refresh_token
            else:
                return redirect(_LOGIN, code=302)

        # Make header
        headers = {key: value for (key, value) in request.headers if key != 'Host'}

        # Add bearer auth
        if token_is_fresh(s.token):
            headers['Authorization'] = f"Bearer {s.token}"
        else:
            return redirect(_LOGIN, code=302)

        # Make request to the target URL
        # Documentation: https://readthedocs.org/projects/requests/downloads/pdf/latest/
        resp = requests.request(
            headers=headers,
            method=request.method,
            url=url,
            params=request.args,
            data=request.get_data(),
            cookies=request.cookies,
            verify=ssl_cert_verification(),
            allow_redirects=False)

        # Return the response content and status code
        excluded_headers = ['content-encoding', 'content-length', 'transfer-encoding', 'connection']
        headers = [(name, value) for (name, value) in resp.raw.headers.items() if name.lower() not in excluded_headers]
        return Response(resp.content, resp.status_code, headers)

    except Exception as e:
        logger.error(e, exc_info=True)


def refresh_keycloak_token(refresh_token):
    keycloak = cfg['keycloak']
    keycloak_url = keycloak.get('keycloak_url')
    protocol = keycloak.get('protocol')
    realm = keycloak.get('realm')

    url = f"{keycloak_url}/realms/{realm}/protocol/{protocol}/token"
    payload = {
        "client_id": keycloak.get('client_id'),
        "client_secret": keycloak.get('client_secret'),
        "refresh_token": refresh_token,
        "grant_type": "refresh_token"
    }
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    response = requests.post(url, data=payload, headers=headers, verify=ssl_cert_verification())
    if response.status_code == 200:
        tokens = response.json()
        return tokens["access_token"], tokens["refresh_token"]
    else:
        return None, None


def get_keycloak_token(username, password):
    keycloak = cfg['keycloak']
    keycloak_url = keycloak.get('keycloak_url')
    protocol = keycloak.get('protocol')
    realm = keycloak.get('realm')

    url = f"{keycloak_url}/realms/{realm}/protocol/{protocol}/token"
    logger.debug(f"POST {url}")
    payload = {
        "client_id": keycloak.get('client_id'),
        "client_secret": keycloak.get('client_secret'),
        "username": username,
        "password": password,
        "grant_type": "password"
    }
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    response = requests.post(url, data=payload, headers=headers, verify=ssl_cert_verification())
    if response.status_code == 200:
        return response.json()["access_token"], response.json()["refresh_token"]
    else:
        logger.error(f"get_keycloak_token FAILED! Response status code: {response.status_code}", exc_info=True)
        return None, None


def token_is_fresh(token):
    try:
        decoded_token = jwt.decode(token, options={"verify_signature": False})
        exp = decoded_token.get('exp')
        if exp and exp < time.time():
            return False
        return True
    except jwt.ExpiredSignatureError:
        return False
    except jwt.DecodeError:
        return False


def ssl_cert_verification():
    if GENERAL.getboolean('tls_enabled', False):
        return GENERAL.get('cert_file')
    else:
        return False


@flask.route(_LOGIN)
def login():
    global APP_VERSION

    s = get_current_session()
    wrong_password = False
    username = ""
    if s is not None:
        username = s.username if s.username else ""
        wrong_password = s.wrong_password

    return render_template('login.html',
                           username=username,
                           wrong_password=wrong_password,
                           app_version=APP_VERSION,
                           profile=ACTIVE_PROFILE)


@flask.route(_TOKEN)
def token():
    s = get_current_session()
    if s is None or s.token is None:
        return redirect(_LOGIN, code=302)
        # Make request to the target URL

    # Make header
    headers = {key: value for (key, value) in request.headers if key != 'Host'}

    # Add bearer auth
    if s.token is not None:
        headers['Authorization'] = f"Bearer {s.token}"

    swagger_url = f"{GENERAL.get('swagger_url')}"

    return render_template('token.html',
                           jwt_token=s.token,
                           swagger_url=swagger_url,
                           session_key=SESSION_ID_KEY,
                           session_id=request.cookies.get(SESSION_ID_KEY)[:36])


@flask.route(_LOGOUT_BASE_PATH)
@flask.route(_LOGOUT_ADDITIONAL)
def logout():
    global SESSIONS
    global LAST_REQUESTED_URL
    global APP_VERSION

    LAST_REQUESTED_URL = None
    session_id = request.cookies.get(SESSION_ID_KEY)
    if session_id is not None:
        s = get_session(session_id)
        username = s.username if s is not None and s.username else ""
        if session_id in SESSIONS:
            del SESSIONS[session_id]
    else:
        username = ""

    return render_template('login.html', username=username,
                           app_version=APP_VERSION, profile=ACTIVE_PROFILE )


@flask.route(_AUTH, methods=['POST'])
def auth():
    global SESSIONS
    global LAST_REQUESTED_URL
    global REDIRECT_URL

    try:
        s = Session()
        s.time = int(time.time())
        s.username = request.values.get("username")
        s.password = request.values.get("password")
        s.token, s.refresh_token = get_keycloak_token(s.username, s.password)
        # Make random SESSION_IS:
        if s.token:
            s.id = f"{secrets.token_hex(nbytes=16)[:22]}..|{int(time.time()) }|{s.token}"
            s.wrong_password = False
            dt = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            logger.info(f"Created new session for user {s.username} at {dt}. Set cookie {SESSION_ID_KEY}='{s.id[:36]}...'")
            redirect_to = LAST_REQUESTED_URL if LAST_REQUESTED_URL is not None else REDIRECT_URL
        else:
            s.id = "-1"
            s.wrong_password = True
            redirect_to = _LOGIN
            logger.warning(f"Wrong username or password. Redirected back to {redirect_to}")

        SESSIONS[s.id] = s

        response = make_response(redirect(redirect_to, code=302))
        response.set_cookie(SESSION_ID_KEY, f"{s.id}")
        LAST_REQUESTED_URL = None
        return response

    except Exception as e:
        logger.error(e, exc_info=True)
        return None


@flask.route('/health', methods=['GET'])
def health_check():
    """Проверка состояния приложения"""
    return jsonify({
        "status": "OK",
        "message": "Application is healthy"
    }), 200

@flask.route('/ready', methods=['GET'])
def readiness_check():
    """Проверка готовности приложения к работе"""
    try:
        keycloak_service_ready = check_keycloak_service()

        if keycloak_service_ready:
            return jsonify({
                "status": "OK",
                "message": "Application is ready to handle requests",
                "dependencies": {
                    "database": "available",
                    "external_service": "available"
                }
            }), 200
        else:
            return jsonify({
                "status": "SERVICE_UNAVAILABLE",
                "message": "Application is not ready",
                "dependencies": {
                    "keycloak_service": "unavailable" if not keycloak_service_ready else "available"
                }
            }), 503
    except Exception as e:
        return jsonify({
            "status": "ERROR",
            "message": f"Readiness check failed: {str(e)}"
        }), 500

def check_keycloak_service():
    """Проверка доступности Keycloak через его health-эндпоинт"""
    try:
        # Получаем URL Keycloak из конфига
        keycloak = cfg['keycloak']
        keycloak_url = keycloak.get('keycloak_url')

        if not keycloak_url:
            return False

        # Формируем URL для проверки health (стандартный эндпоинт Keycloak)
        health_url = f"{keycloak_url.rstrip('/')}/health"

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

        logger.warning(f"Keycloak health check failed: {health_url} -> {response.status_code}")
        return False

    except RequestException as e:
        logger.error(f"Keycloak health check failed: {str(e)}")
        return False
    except ValueError as e:
        logger.error(f"Invalid JSON response from Keycloak: {str(e)}")
        return False


def init_log():
    log = cfg['log']
    logdir = log.get('dir', '')
    handlers = [logging.StreamHandler()]

    if len(logdir) > 0:
        filename = f"{logdir}/{os.path.basename(__file__)}.log"
        # Create parent dir if not exists
        Path(logdir).mkdir(parents=True, exist_ok=True)
        handlers.append( logging.FileHandler(filename))

    level = log.get('level', 'INFO')
    if 'DEBUG' == level.upper():
        level = logging.DEBUG
    elif 'WARN' == level.upper():
        level = logging.WARN
    elif 'ERROR' == level.upper():
        level = logging.ERROR
    elif 'FATAL' == level.upper():
        level = logging.FATAL
    else:
        level = logging.INFO

    logging.basicConfig(level=level,
                        format='%(asctime)s - %(name)s [%(levelname)s]: %(message)s',
                        handlers=handlers)

if __name__ == '__main__':
    main()

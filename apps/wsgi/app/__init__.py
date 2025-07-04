from flask import Flask
from typing import Optional, Dict, Any

def create_app():
    app = Flask(__name__)

    # # Инициализация расширений
    # from .extensions import db, cors
    # db.init_app(app)
    # cors.init_app(app)

    # Регистрация blueprint'ов
    from .routes.web_forms import web_bp
    from .routes.auth import auth_bp
    from .routes.api import api_bp
    from .routes.state import state_bp

    app.register_blueprint(web_bp)
    app.register_blueprint(auth_bp, url_prefix='/auth')
    app.register_blueprint(api_bp, url_prefix='/api/v1')
    app.register_blueprint(state_bp)

    return app
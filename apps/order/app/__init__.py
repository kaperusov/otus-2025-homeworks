from flask import Flask

def create_app():
    app = Flask(__name__)

    # Регистрация blueprint'ов
    from .routes.state import state_bp
    from .routes.api import api_bp

    app.register_blueprint(state_bp)
    app.register_blueprint(api_bp, url_prefix='/api/v1')

    return app
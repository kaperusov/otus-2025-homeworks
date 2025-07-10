import argparse
import sys
from pathlib import Path

from __version import __version__

from app import create_app
from app.config import ConfigReader
from app.config_store import ConfigStore
from app.logger import init_log
from app.logger_store import LoggerStore


class LazyApplication:
    """Proxy для отложенной инициализации приложения"""
    _app = None

    def __call__(self, *args, **kwargs):
        if self._app is None:
            self._app = initialize_application()
        return self._app(*args, **kwargs)

# Для Gunicorn
application = LazyApplication()



def parse_args(args=None):
    """Парсинг аргументов командной строки"""
    parser = argparse.ArgumentParser(description='Run the application server')
    parser.add_argument("-c", "--config",
                        help="path to configuration file",
                        type=str)
    parser.add_argument("-v", "--version",
                        help="show version and exit",
                        action='store_true')
    return parser.parse_args(args)

def show_version_and_exit():
    """Выводит версию и завершает работу"""
    print(f"{__version__}")
    sys.exit(0)


def get_default_config_path():
    """Возвращает путь к конфигу по умолчанию"""
    default_paths = [
        'config.ini',
        'app/config.ini',
        '/etc/app/config.ini'
    ]
    for path in default_paths:
        if Path(path).exists():
            return path
    return None

def initialize_application(config_path=None):
    """Инициализация приложения с конфигурацией"""
    # Определяем путь к конфигу
    effective_config_path = config_path or get_default_config_path()

    if not effective_config_path:
        print("Error: No configuration file found", file=sys.stderr)
        sys.exit(1)

    # Инициализация конфига
    try:
        config = ConfigReader(config_file=effective_config_path)
    except Exception as e:
        print(f"Error loading config: {e}", file=sys.stderr)
        sys.exit(1)

    ConfigStore.set_config(config)

    # Инициализация логгера
    logger = init_log(app_name=config.APP_NAME,
                      log_config=config.get_section(config.LOGGING))
    LoggerStore.set_logger(logger)

    # Создание Flask-приложения
    app = create_app()

    # Конфигурация сервера
    app.config.update({
        'HOST': config.get_value('SERVER', 'host', '0.0.0.0'),
        'PORT': config.get_int('SERVER', 'port', 5000),
        'DEBUG': config.get_boolean('SERVER', 'debug', False)
    })

    # Инициализация логгера
    logger = init_log(app_name=config.APP_NAME,
                      log_config=config.get_section(config.LOGGING))

    logger.info(f"Application initialized. Version: {__version__}")
    return app

def run_development_server(app):
    """Запуск сервера для разработки"""
    from waitress import serve
    if app.config['DEBUG']:
        app.run(
            host=app.config['HOST'],
            port=app.config['PORT'],
            debug=app.config['DEBUG']
        )
    else:
        serve(
            app,
            host=app.config['HOST'],
            port=app.config['PORT']
        )

def main():
    """Точка входа для запуска из командной строки"""
    args = parse_args()

    # Обработка запроса версии
    if args.version:
        show_version_and_exit()

    # Обработка запроса справки
    if '-h' in sys.argv or '--help' in sys.argv:
        parse_args().print_help()
        sys.exit(0)

    # Инициализация и запуск
    app = initialize_application(args.config)
    run_development_server(app)

if __name__ == '__main__':
    main()
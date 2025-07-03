import argparse

import app
from waitress import serve
from app.config import ConfigReader
from app._version import __version__
from app.config_store import ConfigStore
from app.logger import init_log
from app.logger_store import LoggerStore
from app.services.session_manager import SessionManager


def main():
    # Argument parser
    parser = argparse.ArgumentParser()
    parser.add_argument("-c", "--config",
                        help="specify path to configuration file",
                        type=str)
    parser.add_argument("-v", "--version",
                        help="show version", default=False, action='store_true')
    args = parser.parse_args()

    if args.version:
        print(f"{__version__}")
        exit(0)

    # Чтение конфигурации
    config = ConfigReader(config_file=args.config) if args.config else ConfigReader()
    ConfigStore.set_config(config)

    # Инициализируем logger
    logger = init_log(app_name=config.APP_NAME, log_config=config.get_section(config.LOGGING))
    logger.debug( f"Loaded config file: {config.config_file}")
    LoggerStore.set_logger(logger)

    # Инициализируем SessionManager с настройками
    session_manager = SessionManager(token_expiry_minutes=30)
    logger.debug( f"Create new sessions pull: {session_manager.get_sessions()}")


    server = app.create_app()
    host = config.get_value(config.GENERAL, 'host', 'localhost')
    port = config.get_value(config.GENERAL, 'port', '5000')
    debug = config.get_boolean(config.GENERAL, 'debug_mode', False)

    logger.info( f"Starting service {config.APP_NAME}, version: {__version__} {'(debug mode ON)' if debug else ''}" )

    if config.is_debug_mode():
        # For development
        server.run(host=host, port=port, debug=debug)
    else:
        # For production
        serve(server, host=host, port=port)
        logger.info( "Flask server on finished. Bye.")
        logger.info( "***" )


if __name__ == '__main__':
    main()
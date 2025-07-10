import configparser
from pathlib import Path
from typing import Optional, Dict, Any
from typing import Final

class ConfigReader:

    APP_NAME: Final[str] = "Order Service" # Web Server Gateway Interface

    # Sections
    GENERAL   : Final[str] = "GENERAL"
    SECURITY  : Final[str] = "SECURITY"
    API       : Final[str] = "API"
    KEYCLOAK  : Final[str] = "KEYCLOAK"
    LOGGING   : Final[str] = "LOGGING"

    def __init__(self, config_file: str = 'config.ini'):
        if not Path(config_file).exists():
            print( f"The configuration file {config_file} not found. Exit with error 1")
            exit( 1 )
        self.config_file = config_file
        self.config = configparser.ConfigParser()
        self.config.read(self.config_file)

    def is_debug_mode(self):
        return self.get_boolean(ConfigReader.GENERAL, 'debug_mode', False)

    def get_section(self, section_name: str) -> Optional[Dict[str, str]]:
        """Получить все параметры из секции конфига"""
        if section_name in self.config:
            return dict(self.config[section_name])
        return None

    def get_value(self, section_name: str, key: str, default: Any = None) -> Optional[str]:
        """Получить конкретное значение из конфига"""
        try:
            return self.config.get(section_name, key)
        except (configparser.NoSectionError, configparser.NoOptionError):
            return default

    def get_int(self, section_name: str, key: str, default: int = 0) -> int:
        """Получить целочисленное значение"""
        try:
            return self.config.getint(section_name, key)
        except (configparser.NoSectionError, configparser.NoOptionError, ValueError):
            return default

    def get_boolean(self, section_name: str, key: str, default: bool = False) -> bool:
        """Получить булево значение"""
        try:
            return self.config.getboolean(section_name, key)
        except (configparser.NoSectionError, configparser.NoOptionError, ValueError):
            return default

    def get_float(self, section_name: str, key: str, default: float = 0.0) -> float:
        """Получить значение с плавающей точкой"""
        try:
            return self.config.getfloat(section_name, key)
        except (configparser.NoSectionError, configparser.NoOptionError, ValueError):
            return default

    def reload(self) -> None:
        """Перезагрузить конфигурацию из файла"""
        self.config.read(self.config_file)
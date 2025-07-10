class ConfigStore:
    _config = None

    @classmethod
    def set_config(cls, config):
        cls._config = config

    @classmethod
    def get_config(cls, section: str = None):
        if cls._config is None:
            raise RuntimeError("Configuration not initialized")

        if section:
            return cls._config.get_section(section)

        return cls._config
class LoggerStore:
    _logger = None

    @classmethod
    def set_logger(cls, config):
        cls._logger = config

    @classmethod
    def get_logger(cls):
        if cls._logger is None:
            raise RuntimeError("Logger not initialized")
        return cls._logger
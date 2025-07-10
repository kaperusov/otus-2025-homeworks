import logging
from pathlib import Path
from typing import Dict

def init_log(app_name: str, log_config: Dict[str, str]):
    logdir = log_config.get('dir', '')
    handlers = [logging.StreamHandler()]

    if len(logdir) > 0:
        filename = f"{logdir}/{app_name}.log"
        # Create parent dir if not exists
        Path(logdir).mkdir(parents=True, exist_ok=True)
        handlers.append( logging.FileHandler(filename))

    level = log_config.get('level', 'INFO')
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

    return logging.getLogger(app_name)
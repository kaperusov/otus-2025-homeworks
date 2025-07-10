from flask import jsonify, Response
from typing import Any, Dict, Optional, Union, List

from app.logger_store import LoggerStore


def make_response(
        code: int,
        message: str,
        data: Optional[Union[Dict, List]] = None,
        **kwargs: Any
) -> tuple[Response, int]:
    """
    Создает стандартизированный JSON-ответ для API

    :param code: HTTP статус-код
    :param message: Человекочитаемое сообщение
    :param data: Основные данные ответа (опционально)
    :param kwargs: Дополнительные поля ответа
    :return: Кортеж (Response, HTTP код)
    """
    status_mapping = {
        200: "OK",
        201: "Created",
        204: "No Content",
        400: "Bad Request",
        401: "Unauthorized",
        403: "Forbidden",
        404: "Not Found",
        409: "Conflict",
        422: "Unprocessable Entity",
        429: "Too Many Requests",
        500: "Internal Server Error",
        502: "Bad Gateway",
        503: "Service Unavailable"
    }

    response = {
        "status": status_mapping.get(code, "Unknown Status"),
        "message": message,
    }

    if data is not None:
        response["data"] = data

    # Добавляем дополнительные поля
    response.update(kwargs)
    j = jsonify(response)
    if code > 300:
        logger = LoggerStore.get_logger()
        logger.error(f"Send response: {code}, {response}")
    return j, code

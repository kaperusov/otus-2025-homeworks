import uuid
import logging
from datetime import datetime, timedelta
from typing import Optional, Dict

from app.logger_store import LoggerStore



class SessionManager:
    _instance = None

    def __new__(cls, token_expiry_minutes: int = 30):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._sessions: Dict[str, Dict] = {}
            cls._instance._logger = LoggerStore.get_logger()
            cls._instance.token_expiry = token_expiry_minutes * 60 # в секундах

        return cls._instance

    def get_sessions(self):
        return self._sessions

    def create_session(self, username: str, access_token: str) -> str:
        """Создает новую сессию и возвращает session_id"""
        session_id = str(uuid.uuid4())
        self._sessions[session_id] = {
            'username': username,
            'access_token': access_token,
            'created_at': datetime.now(),
            'expires_at': datetime.now() + timedelta(seconds=self.token_expiry)
        }
        self._logger.debug( f"Created a new session '{session_id}' for username: '{username}'" )
        return session_id

    def get_session(self, session_id: str) -> Optional[Dict]:
        """Получает данные сессии по session_id"""
        if session_id not in self._sessions:
            return None

        session = self._sessions[session_id]

        # Проверяем не истекла ли сессия
        if datetime.now() > session['expires_at']:
            self.delete_session(session_id)
            return None

        return session

    def get_session_by_username(self, username: str) -> Optional[Dict]:
        """Возвращает сессию по username или None если не найдена"""
        for session_id, session_data in self._sessions.items():
            if session_data['username'] == username:
                session = self.get_session(session_id)
                if session:
                    self._logger.debug( f"Return exists session for user {username}" )
                return session
        return None

    def get_access_token(self, session_id: str) -> Optional[str]:
        """Получает access_token по session_id"""
        session = self.get_session(session_id)
        return session['access_token'] if session else None

    def get_username(self, session_id: str) -> Optional[str]:
        """Получает username по session_id"""
        session = self.get_session(session_id)
        return session['username'] if session else None

    def refresh_session(self, session_id: str) -> bool:
        """Обновляет время жизни сессии"""
        if session_id not in self._sessions:
            return False

        self._sessions[session_id]['expires_at'] = datetime.now() + timedelta(seconds=self.token_expiry)
        return True

    def delete_session(self, session_id: str) -> None:
        """Удаляет сессию"""
        if session_id in self._sessions:
            del self._sessions[session_id]

    def cleanup_expired_sessions(self) -> None:
        """Очищает истекшие сессии"""
        now = datetime.now()
        expired = [sid for sid, session in self._sessions.items() if session['expires_at'] < now]
        for sid in expired:
            self.delete_session(sid)
FROM python:3.11.7-slim

WORKDIR /app

COPY app                .
COPY wsgi.py            .
COPY requirements.txt   .

RUN pip3 install -r requirements.txt

ENTRYPOINT [ "python3" ]
CMD [ "wsgi.py" ]

EXPOSE 5000
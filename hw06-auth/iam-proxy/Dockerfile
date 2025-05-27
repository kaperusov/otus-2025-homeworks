FROM python:3.11.7-slim

WORKDIR /app

COPY static             ./static
COPY templates          ./templates
COPY front-app.py       .
COPY requirements.txt   .
COPY .version           .

RUN pip3 install -r requirements.txt

ENTRYPOINT [ "python3" ]
CMD [ "front-app.py" ]
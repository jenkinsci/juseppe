FROM maven
MAINTAINER Merkushev Kirill (github:lanwen)

ENV JUSEPPE_BASE_DIR            /juseppe
ENV JUSEPPE_CERT_DIR            ${JUSEPPE_BASE_DIR}/cert
ENV JUSEPPE_CERT_PATH           ${JUSEPPE_CERT_DIR}/uc.crt
ENV JUSEPPE_PRIVATE_KEY_PATH    ${JUSEPPE_CERT_DIR}/uc.key

ENV JUSEPPE_PLUGINS_DIR         ${JUSEPPE_BASE_DIR}/plugins
ENV JUSEPPE_SAVE_TO_DIR         ${JUSEPPE_BASE_DIR}/json

ENV JUSEPPE_BASE_URI            http://localhost:8080
ENV JUSEPPE_BIND_PORT           8080

RUN mkdir ${JUSEPPE_BASE_DIR} \
    && mkdir ${JUSEPPE_PLUGINS_DIR} \
    && mkdir ${JUSEPPE_CERT_DIR} \
    && mkdir ${JUSEPPE_SAVE_TO_DIR}
    
ADD . ${JUSEPPE_BASE_DIR}
WORKDIR ${JUSEPPE_BASE_DIR}

#Locally can be replaced with "mvn package && docker build ..." to avoid downloading lot of jars
RUN ["mvn", "package", "-Dmaven.test.skip=true"]

# Self-signed certificate
RUN openssl genrsa -out ${JUSEPPE_PRIVATE_KEY_PATH} 2048 \
&& openssl req -nodes -x509 -new \
    -key ${JUSEPPE_PRIVATE_KEY_PATH} \
    -out ${JUSEPPE_CERT_PATH} \
    -days 1056 \
    -subj "/C=EN/ST=Update-Center/L=Juseppe/O=Juseppe"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "juseppe-cli/target/juseppe.jar"]
CMD ["-w", "serve"]

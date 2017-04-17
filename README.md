# Juseppe - Jenkins Update Site Embedded for Plugin Publishing

## How to launch with help of docker
[![](https://badge.imagelayers.io/lanwen/juseppe:latest.svg)](https://imagelayers.io/?images=lanwen/juseppe:latest 'imagelayers.io')

Run it with mounted plugins folder as volume. Remember to set `JUSEPPE_BASE_URI` env var

```
docker run --name juseppe -v /your/plugins/dir/:/juseppe/plugins/ -e JUSEPPE_BASE_URI=http://my.company.com -p 80:8080 lanwen/juseppe
```

Then it will be available on `http://dockerhost:80/update-center.json`

### Built-in self-signed certificate

Certificate can be copied from json in format:

```
-----BEGIN CERTIFICATE-----
{json value of signature.certificates[0] (without quotes)}
-----END CERTIFICATE-----
```

**WARN!** Certificate regenerates in every new docker image!

### Specify own certificate

Mount as volumes private key and cert:

```
docker run --name juseppe -v /your/private/key:/juseppe/cert/uc.key -v /your/cert/file:/juseppe/cert/uc.crt ... lanwen/juseppe
```

### Just generate json

```
docker run --rm ... lanwen/juseppe generate
```

## Build new image  

`docker build -t juseppe:source .`

# Without docker

## 1. Checkout & Build 

With maven just run `mvn package`, and you will find jar in `target/juseppe.jar`

## 2. Generate self-signed cert with private key

and point juseppe to use it with help of `-Djuseppe.certificate=path/to/cert.crt` and `-Djuseppe.private.key=path/to/priv.key` 

```
openssl genrsa -out uc.key 2048 \
&& openssl req -nodes -x509 -new \
    -key uc.key \
    -out uc.crt \
    -days 1056 \
    -subj "/C=EN/ST=Update-Center/L=Juseppe/O=Juseppe"
```
 
## 3. Run help to see all available commands

`java -jar juseppe.jar help`

#### 3.1 Generate new json

`java -jar juseppe.jar generate`

#### 3.2 Serve json and plugins with help of jetty server

To run server with file watching in current directory (not the dir where jar located!)

`java -jar juseppe.jar serve`

### 3.3 Watch for changes automatically

Just add `-w` (or `--watch`) flag to update jsons on any plugin list changes

`java -jar juseppe.jar -w serve` or `java -jar juseppe.jar -w generate`

You can also change plugin dir location by specifying `-p` (or `--plugins-directory`) flag with location of directory where the plugins are

`java -jar juseppe.jar -w -p /tmp/plugins serve`


## 4. Configure 

You can define system properties or environment vars to override default behaviour. 
Complete list of vars can be found after `juseppe env` command.

- `JUSEPPE_CERT_PATH` (`juseppe.certificate`)   
  path of certificate (must be used in pair with private key prop). Defaults to *uc.crt* 

- `JUSEPPE_PRIVATE_KEY_PATH` (`juseppe.private.key`)   
  path of private key (must be used in pair with cert). Defaults to *uc.key*

- `JUSEPPE_PLUGINS_DIR` (`juseppe.plugins.dir`)   
  where the plugins are. Searches only `*.hpi` and `*.jpi`. Defaults to *current working dir*

- `JUSEPPE_SAVE_TO_DIR` (`juseppe.saveto.dir`)   
  where to save generated json file. Defaults to *current working dir*

- `JUSEPPE_UC_JSON_NAME` (`juseppe.uc.json.name`)   
  name of generated update center json file. Defaults to `update-center.json`

- `JUSEPPE_RELEASE_HISTORY_JSON_NAME` (`juseppe.release.history.json.name`)   
  name of generated release-history json file. Defaults to `release-history.json`

- `JUSEPPE_BASE_URI` (`juseppe.baseurl`)   
  url to prepend for plugins download link in json. Defaults to `http://localhost:8080`

- `JUSEPPE_UPDATE_CENTER_ID` (`juseppe.update.center.id`)   
  id of the update center. Must be unique inside of jenkins. Defaults to `juseppe`

- `JUSEPPE_BIND_PORT` (`juseppe.jetty.port`)   
  port for juseppe file server. Defaults to `8080`

Example: 

`java -jar -Djuseppe.saveto.dir=/tmp/update/ juseppe.jar -w serve` or `JUSEPPE_SAVE_TO_DIR=/tmp/update/ java -jar juseppe.jar -w serve`

Properties are overridden in order: *default value* -> *env vars* -> *system properties* -> *direct cli arguments* 

## How to connect Jenkins

Site can be added with help of: 
    
- [UpdateSites Manager plugin](https://wiki.jenkins-ci.org/display/JENKINS/UpdateSites+Manager+plugin)



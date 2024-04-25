# Spring Cloud Function + AWS Lambda
Spring Cloud Function + AWS Lambda Demo

[Spring Boot 3 + AWS Lambda Demo](https://github.com/davidy87/spring-boot-aws-lambda)에 이어, Spring Cloud Function으로 AWS Lambda를 연동하는 예제 프로젝트

## References
* https://docs.spring.io/spring-cloud-function/reference/adapters/aws-intro.html
* https://cloud.spring.io/spring-cloud-function/reference/html/aws.html

<br>

# Getting Started

## Pre-requisites
* [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
* [SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html)
* [Gradle](https://gradle.org/)

<br>

## 1. build.gradle 설정
모든 설정은 아래의 링크들을 참고했다.
* https://docs.spring.io/spring-cloud-function/reference/adapters/aws-intro.html
* https://cloud.spring.io/spring-cloud-function/reference/html/aws.html


### Spring Clound 버전 지정
의존성 및 shadowJar에 사용할 Spring Cloud의 버전을 설정
```groovy
ext {
    set('springCloudVersion', "2023.0.1")
}
```


### 의존성 추가
```groovy
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-function-web'
    implementation 'org.springframework.cloud:spring-cloud-function-adapter-aws'
    implementation 'com.amazonaws:aws-lambda-java-core:1.2.1'
    implementation 'com.amazonaws:aws-lambda-java-events:3.8.0'
    
    // 생략
}
```

### 플러그인 추가
thinJar, shadowJar 빌드를 위한 플러그인을 추가
```groovy
plugins {
    // 생략
    id 'com.github.johnrengelman.shadow' version '8.1.1'
    id 'org.springframework.boot.experimental.thin-launcher' version "1.0.31.RELEASE"
}
```

### shadowJar 빌드 설정
```groovy
assemble.dependsOn = [thinJar, shadowJar]

shadowJar.mustRunAfter thinJar

import com.github.jengelman.gradle.plugins.shadow.transformers.*

shadowJar {
    archiveClassifier = 'aws'
    manifest {
        inheritFrom(project.tasks.thinJar.manifest)
    }
    dependencies {
        exclude(
                dependency("org.springframework.cloud:spring-cloud-function-web:${springCloudVersion}"))
    }
    // Required for Spring
    mergeServiceFiles()
    append 'META-INF/spring.handlers'
    append 'META-INF/spring.schemas'
    append 'META-INF/spring.tooling'
    append 'META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports'
    append 'META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports'
    transform(PropertiesFileTransformer) {
        paths = ['META-INF/spring.factories']
        mergeStrategy = "append"
    }
}
```

### 참고사항
* AWS Lambda에 배포 시, `spring-cloud-function-web` 의존성은 필요하지 않기 때문에 빌드 대상에서 제외시켰다.

<br>

## 2. 예제 코드 작성

### Function
API 역할을 하는 함수형 인터페이스를 만들고 빈으로 등록해야 한다.
```java
@Component
public class Hello implements Function<String, String> {

    @Override
    public String apply(String s) {
        return "Hello, " + s + "!";
    }
}
```
* API의 기능에 맞게 `Function`, `Consumer`, `Supplier` 등록 가능
* API 경로는 bean의 이름
  - 위의 예제의 경우, `/hello` 가 API 요청 경로가 된다.

### FunctionInvoker
이후에 `template.yml`에 지정할 Handler 클래스이다. 단순히 편의성을 위해 구현했다.
```java
public final class LambdaHandler extends FunctionInvoker {
}
```
* 대신에 `org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest`를 그대로 Handler로 지정해도 무방하다.

<br>

## 2. 빌드
gradle build 실행 -> `build/libs/{jar 파일}` 생성
```shell
./gradlew clean build
```

<br>

## 3. template.yml 생성

### 참고 사항
```yaml
CodeUri: build/libs/{jar 파일}
```
* `CodeUri` 항목에 gradle build로 생성된 jar 파일의 경로를 지정한다.

### template.yml

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: Spring Cloud + AWS Lambda demo project

Globals:
  Api:
    # API Gateway regional endpoints
    EndpointConfiguration: REGIONAL

Resources:
  DemoCloudFunction:
    Type: AWS::Serverless::Function
    Properties:
      Handler: demo.springcloud.lambda.handler.LambdaHandler::handleRequest
      Runtime: java17
      CodeUri: build/libs/spring-cloud-aws-lambda-0.0.1-SNAPSHOT-aws.jar
      Architectures:
        - x86_64
      MemorySize: 2048
      Policies: AWSLambdaBasicExecutionRole
      Timeout: 60
      SnapStart:
        ApplyOn: PublishedVersions
      AutoPublishAlias: prod
      Events:
        HelloWorld:
          Type: Api
          Properties:
            Path: /{proxy+}
            Method: ANY

Outputs:
  DemoCloudApplicationApi:
    Description: URL for application
    Value: !Sub 'https://${ServerlessRestApi}.execute-api.${AWS::Region}.amazonaws.com'
    Export:
      Name: DemoCloudApplicationApi
```

<br>

## 3. 빌드 및 배포

### 빌드
이미 배포할 jar 파일을 `template.yml`에 지정했기 때문에 별도의 빌드 과정은 필요하지 않다.

### Local에 함수 호출 테스트
```shell
sam local start-api
```
* Local 사용 시, Docker 설치 및 실행 필수
* Docker를 실행했는데도 오류가 생긴다면? --> [해결 방법](https://github.com/aws/aws-sam-cli/issues/5646)

### AWS Lambda에 배포
```shell
sam deploy --guided
```






// Jenkins
pipeline {
    // 이 파이프라인을 실행할 Jenkins 에이전트(작업 서버)를 지정
    agent any

    // 파이프라인 전체에서 사용할 환경 변수를 정의
    environment {
        // [수정 필요]AWS ECR(Elastic Container Registry) 관련 환경 변수
        // '123456789012'를 실제 팀의 AWS 계정 ID로 변경
        ECR_REGISTRY = "123456789012.dkr.ecr.ap-northeast-2.amazonaws.com"
        // ECR에 생성된 우리 서비스의 리포지토리 이름
        ECR_REPOSITORY = "ai-feature-service"

        // [수정 필요] Helm 차트를 관리하는 Git 리포지토리 주소
        CHART_REPO_URL = "https://github.com/your-team/project-helm-charts.git"
        // [수정 필요] 위 Git 리포지토리에 접근하기 위한 Jenkins 인증 정보(Credential)의 ID
        CHART_REPO_CREDENTIALS_ID = "helm-repo-credentials"
    }

    // 파이프라인이 수행할 실제 작업 단계들을 정의
    stages {
        // stage 1: 소스 코드를 빌드하고 테스트를 실행
        stage('1. 소스 코드 빌드 및 테스트') {
            steps {
                // sh: 쉘 스크립트를 실행
                // './gradlew clean build': 기존 빌드 산출물을 삭제(clean)하고, 전체 프로젝트를 컴파일하고 모든 테스트를 실행(build
                sh "./gradlew clean build"
            }
        }

        // stage 2: Docker 이미지를 빌드하고 AWS ECR에 푸시(업로드)
        stage('2. 도커 이미지 빌드 및 푸시') {
            steps {
                // 여러 Groovy 스크립트를 실행할 수 있는 블록
                script {
                    // git rev-parse --short HEAD: 현재 커밋의 고유 ID(해시) 앞 7자리를 가져옵니다. (예: a1b2c3d)
                    // 이 고유 ID를 이미지 태그로 사용하여, 어떤 코드 버전으로 이미지가 만들어졌는지 명확히 추적
                    def imageTag = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    def fullImageName = "${ECR_REGISTRY}/${ECR_REPOSITORY}:${imageTag}"

                    // Dockerfile을 사용하여 이미지를 빌드
                    sh "docker build -t ${fullImageName} ."

                    // withAWS: Jenkins에 설정된 AWS 인증 정보를 사용하여 AWS 서비스에 접근
                    // (별도의 access key/secret key 없이 IAM 역할을 사용하는 것이 가장 안전)
                    withAWS(region: 'ap-northeast-2') {
                        // AWS ECR에 로그인하기 위한 임시 사용자 이름과 토큰을 생성
                        def ecrLogin = ecrLogin()
                        // 생성된 토큰을 사용하여 Docker 클라이언트를 ECR에 로그인
                        sh "docker login -u ${ecrLogin.user} -p ${ecrLogin.password} https://${ecrLogin.endpoint}"
                        // 빌드된 이미지를 ECR 리포지토리에 푸시
                        sh "docker push ${fullImageName}"
                    }
                }
            }
        }

        // stage 3: 배포 정보를 담고 있는 Helm 차트 Git 리포지토리를 업데이트 (ArgoCD 연동의 핵심)
        stage('3. Helm 차트 업데이트 및 푸시') {
            steps {
                script {
                    // 2단계에서 사용한 이미지 태그를 다시 가져옵니다.
                    def imageTag = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()

                    // dir('helm-charts'): 현재 작업 공간 안에 'helm-charts'라는 임시 디렉토리를 만들고 그 안에서 작업을 수행
                    dir('helm-charts') {
                        // Helm 차트 리포지토리를 클론(복제)
                        git credentialsId: CHART_REPO_CREDENTIALS_ID, url: CHART_REPO_URL

                        // 1. values.yaml 파일의 이미지 태그를 이번에 빌드한 새 이미지 태그로 변경
                        // yq는 YAML 파일을 다루는 커맨드라인 도구 (Jenkins 서버에 설치 필요)
                        sh "yq e '.image.tag = \"${imageTag}\"' -i ./charts/ai-feature-service/values.yaml"

                        // 2. Chart.yaml 파일의 버전을 자동으로 1 올립니다. (예: 0.1.5 -> 0.1.6)
                        sh "yq e '.version = (semver(.version) | bump_patch)' -i ./charts/ai-feature-service/Chart.yaml"
                        def newChartVersion = sh(returnStdout: true, script: "yq e '.version' ./charts/ai-feature-service/Chart.yaml").trim()

                        // 3. 변경된 Helm 차트 내용을 다시 Git 리포지토리에 커밋하고 푸시
                        sh "git config user.email 'jenkins@your-company.com'"
                        sh "git config user.name 'Jenkins CI'"
                        sh "git add ."
                        sh "git commit -m 'Update ai-feature-service to image ${imageTag} (Chart v${newChartVersion})'"
                        sh "git push origin main"
                    }
                }
            }
        }
    }
}
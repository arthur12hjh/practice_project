app - debug.apk 파일을 다운로드 받거나, https://drive.google.com/file/d/1ynA7sZoMvt8UIfShR4SnaQ1EmBDjfjPy/view?usp=sharing 에 접속하면
안드로이드 핸드폰에서 어플리케이션을 실행할 수 있습니다

#프로젝트 소개
카메라 앱 동필이는 다른 필터 카메라 앱과는 다르게 다양한 동물들(개, 고양이)에게도 필터를 적용할 수 있는 특별한 앱입니다. 동필이는 반려동물을 사랑하는 사람들에게 행복한 순간을 간직할 수 있는 기회를 제공합니다. 동필이는 반려동물을 사랑하는 사람들을 위한 앱의 시작을 알립니다.

동필이는 강아지와 고양이의 얼굴에 필터를 씌워주는 애플리케이션입니다. 동필이가 반려동물의 얼굴에 필터를 씌워주기 위해서는 동물의 얼굴을 찾아야 합니다. 이를 위해 객체를 감지하는 모델에 이미지 데이터를 학습시켰습니다. 학습을 시킬 때는 먼저 얼굴을 찾는 bbs 모델, 그리고 얼굴 이미지에서 눈, 코, 귀를 찾는 lmks 모델 두 가지를 사용하여 정확도를 향상시켰습니다. 그리고 PC에서 학습시킨 Keras 모델을 모바일에 적용하기 위해서 Tensorflow lite로 변환하여 안드로이드 스튜디오에 적용하였습니다. 

앱 작동 방식은 Tensorflow lite 모델에 이미지를 입력하여 받아온 좌표에 필터를 씌우는 방식입니다. 먼저 Camera 2api에서 이미지를 받아와 Tensor Image로 변환한 후 얼굴을 찾아주는 bbs 모델에 입력하여 얼굴의 왼쪽 위 좌표와 오른쪽 아래의 좌표를 받아옵니다. 그리고 좌표를 이용해서 원본 이미지에서 얼굴 이미지를 추출하고 다시 Tensor Image로 변환하여 눈, 코, 귀를 찾아주는 lmks 모델에 입력합니다. 최종적으로 동필이는 눈, 코, 귀 좌표를 얻게 되고, 이제 좌표들을 연산하여 적용할 필터 이미지의 크기, 각도를 계산하여 두 이미지를 합쳐서 화면에 출력합니다.

#설치 및 사용 매뉴얼
##1. 설치 매뉴얼
app-debug.apk를 설치합니다. 
혹은 README에 있는
https://drive.google.com/file/d/1ynA7sZoMvt8UIfShR4SnaQ1EmBDjfjPy/view?usp=sharing 
링크를 통해 애플리케이션을 설치합니다.
![1](https://github.com/arthur12hjh/practice_project/assets/90200225/0a2ee7ec-5cdd-4810-a8ba-39f47c49ef85)

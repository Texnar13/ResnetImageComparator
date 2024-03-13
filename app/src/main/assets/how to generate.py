

#-------------------------------------- полурабочий вариант (со встроенной функцией, которой вроде бы нет)

import torch
import torch.nn as nn
import torchvision

# Загрузка предварительно обученной модели ResNet-18
resnet18 = torchvision.models.resnet18(pretrained=True)

# Получение доступа к слою, предшествующему классификационному слою
feature_layer = nn.Sequential(*list(resnet18.children())[:-1])

# Создание новой модели, которая содержит только feature_layer
class FeatureExtractor(nn.Module):
    def __init__(self, feature_layer):
        super(FeatureExtractor, self).__init__()
        self.feature_layer = feature_layer
        self.model = resnet18
    def forward(self, x):
        return self.model(x)
    def myfunc(self, x):
        return self.feature_layer(x)

# Создание экземпляра модели
model = FeatureExtractor(feature_layer)

# Оптимизация модели для мобильных устройств (необязательно, но рекомендуется)
model.eval()
example_input = torch.rand(1, 3, 224, 224)  # Пример входных данных

model = torch.jit.trace(model, example_inputs = example_input)
model._save_for_lite_interpreter("resnet18_traced.ptl")




#--------------------------------------- выходной тензор 512 для resnet18


import torch
import torch.nn as nn
import torchvision
from torch.utils.mobile_optimizer import optimize_for_mobile

# Загрузка предварительно обученной модели ResNet-18
resnet18 = torchvision.models.resnet18(pretrained=True)

# Получение доступа к слою, предшествующему классификационному слою
feature_layer = nn.Sequential(*list(resnet18.children())[:-1])

# Создание новой модели, которая содержит только feature_layer
class FeatureExtractor(nn.Module):
    def __init__(self, feature_layer):
        super(FeatureExtractor, self).__init__()
        self.feature_layer = feature_layer

    def forward(self, x):
        return self.feature_layer(x)

# Создание экземпляра модели
model = FeatureExtractor(feature_layer)

# Оптимизация модели для мобильных устройств (необязательно, но рекомендуется)
model.eval()
example_input = torch.rand(1, 3, 224, 224)  # Пример входных данных



model = torch.jit.trace(model, example_inputs = example_input)
model._save_for_lite_interpreter("resnet18_traced.ptl")




#------------------------------ выходной тензор 512 для resnet34



import torch
import torch.nn as nn
import torchvision
from torch.utils.mobile_optimizer import optimize_for_mobile

# Загрузка предварительно обученной модели ResNet-18
resnet34 = torchvision.models.resnet34(pretrained=True)

# Получение доступа к слою, предшествующему классификационному слою
feature_layer = nn.Sequential(*list(resnet34.children())[:-1])

# Создание новой модели, которая содержит только feature_layer
class FeatureExtractor(nn.Module):
    def __init__(self, feature_layer):
        super(FeatureExtractor, self).__init__()
        self.feature_layer = feature_layer

    def forward(self, x):
        return self.feature_layer(x)

# Создание экземпляра модели
model = FeatureExtractor(feature_layer)

# Оптимизация модели для мобильных устройств (необязательно, но рекомендуется)
model.eval()
example_input = torch.rand(1, 3, 224, 224)  # Пример входных данных



model = torch.jit.trace(model, example_inputs = example_input)
model._save_for_lite_interpreter("resnet34_traced.ptl")


#------------------------------ выходной тензор 2048 для resnet101


import torch
import torch.nn as nn
import torchvision
from torch.utils.mobile_optimizer import optimize_for_mobile

# Загрузка предварительно обученной модели ResNet-18
resnet101 = torchvision.models.resnet101(pretrained=True)

# Получение доступа к слою, предшествующему классификационному слою
feature_layer = nn.Sequential(*list(resnet101.children())[:-1])

# Создание новой модели, которая содержит только feature_layer
class FeatureExtractor(nn.Module):
    def __init__(self, feature_layer):
        super(FeatureExtractor, self).__init__()
        self.feature_layer = feature_layer

    def forward(self, x):
        return self.feature_layer(x)

# Создание экземпляра модели
model = FeatureExtractor(feature_layer)

# Оптимизация модели для мобильных устройств (необязательно, но рекомендуется)
model.eval()
example_input = torch.rand(1, 3, 224, 224)  # Пример входных данных



model = torch.jit.trace(model, example_inputs = example_input)
model._save_for_lite_interpreter("resnet101_traced.ptl")



#---------------------------------- выходной тензор 576 для mobilenet

import torch
import torchvision
from torch.utils.mobile_optimizer import optimize_for_mobile

# Загрузка предобученной модели mobilenet_v3_small
model = torchvision.models.mobilenet_v3_small(pretrained=True)

# Обрезание последнего слоя признаков
model = torch.nn.Sequential(*list(model.children())[:-1])

model.eval()
example = torch.rand(1, 3, 224, 224)
traced_script_module = torch.jit.trace(model, example)
optimized_traced_model = optimize_for_mobile(traced_script_module)
optimized_traced_model._save_for_lite_interpreter("mobilenet_v3_small.ptl")


# Слои (-0)480000 - (-1)576 - (-2)97344

import torch
import torchvision
resnet18 = torchvision.models.resnet18(pretrained=True)
resnet18.eval()
example_inputs = torch.rand(1, 3, 224, 224)
resnet18_traced = torch.jit.trace(resnet18, example_inputs = example_inputs)
resnet18_traced.save("resnet18_traced.pt")
resnet18_traced._save_for_lite_interpreter("resnet18_traced.ptl")



import torch
model = torch.hub.load('pytorch/vision:v0.7.0', 'deeplabv3_resnet50', pretrained=True)
model.eval()
scripted_module = torch.jit.script(model)
# Export full jit version model (not compatible lite interpreter), leave it here for comparison
scripted_module.save("deeplabv3_scripted.pt")
# Export lite interpreter version model (compatible with lite interpreter)
scripted_module._save_for_lite_interpreter("deeplabv3_scripted.ptl")


import itertools
import math

points = [[0,0],[36,40],[116,124],[24,116],[40,12],[4,80],[8,160],[88,112],[32,12],[40,56],[28,160],[140,148],[160,96],[48,124],[12,120]]

min = math.inf
for permutation in list(itertools.permutations(points)):
    l = 0
    for i in range(len(points)-1):
        l += math.sqrt((permutation[i][0]- permutation[i+1][0])**2 + (permutation[i][1] - permutation[i+1][1])**2)
    if l < min:
        min = l
print(min)

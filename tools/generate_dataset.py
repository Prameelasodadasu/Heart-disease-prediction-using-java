"""Generates a synthetic dataset with the same columns as the UCI Heart Disease (Cleveland) data.
Replace src/main/resources/data/heart.csv with the real UCI/Kaggle file (same 14 columns) for real results."""
import random
random.seed(7)
cols="age,sex,cp,trestbps,chol,fbs,restecg,thalach,exang,oldpeak,slope,ca,thal,target"
def clip(v,a,b): return max(a,min(b,v))
def ch(p): return random.choices(range(len(p)),p)[0]
rows=[]
for _ in range(900):
    t=1 if random.random()<0.46 else 0
    age=clip(round(random.gauss(51+5*t,9)),29,77)
    sex=int(random.random()<0.55+0.22*t)
    cp=ch([0.06,0.11,0.23,0.60] if t else [0.24,0.25,0.38,0.13])
    bp=clip(round(random.gauss(129+6*t,16)),94,200)
    chol=clip(round(random.gauss(243+8*t,50)),126,564)
    fbs=int(random.random()<0.14+0.03*t)
    ecg=ch([0.40,0.56,0.04] if t else [0.55,0.43,0.02])
    hr=clip(round(random.gauss(157-17*t,19)),71,202)
    ex=int(random.random()<0.17+0.33*t)
    op=clip(round(random.expovariate(1/(0.65+0.85*t)),1),0,6.2)
    sl=ch([0.15,0.70,0.15] if t else [0.55,0.38,0.07])
    ca=ch([0.40,0.30,0.20,0.10] if t else [0.75,0.15,0.07,0.03])
    th=[1,2,3][ch([0.30,0.10,0.60] if t else [0.76,0.05,0.19])]
    rows.append([age,sex,cp,bp,chol,fbs,ecg,hr,ex,op,sl,ca,th,t])
with open("src/main/resources/data/heart.csv","w") as f:
    f.write(cols+"\n")
    for r in rows: f.write(",".join(map(str,r))+"\n")

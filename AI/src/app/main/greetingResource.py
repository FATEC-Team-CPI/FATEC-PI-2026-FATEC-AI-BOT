from fastapi import FastAPI

app = FastAPI()


    
@app.route('/aibot/', methods=['GET'])
def read_root():
    return {"Hello World"}


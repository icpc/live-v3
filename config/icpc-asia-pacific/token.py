import requests
import json

def api_req():
    url = "http://210.245.6.70:8001/auth/login"
    body = {
      "username": "login",
      "password": "password"
    }
    headers = {'Content-type': 'application/json'}

    result = requests.post(url, data = body)
    
    print(result.status_code) 
    rr = result.json()
    print("accessToken: ")
    print(rr['accessToken'])

api_req()

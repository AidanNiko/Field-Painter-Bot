from fastapi import FastAPI, Header, HTTPException, Query
from pymongo import MongoClient
from dotenv import load_dotenv
import os

load_dotenv()

app = FastAPI()

MONGO_URI = os.getenv("MONGO_URI")
API_KEY = os.getenv("API_KEY")

import certifi

client = MongoClient(MONGO_URI, tls=True, tlsCAFile=certifi.where())

try:
    client.admin.command("ping")
    print("MongoDB connected successfully!")
except Exception as e:
    print("Connection failed:", e)


db = client.get_database("Fields")  # specify your database name


@app.get("/status")
def status():
    try:
        # Lightweight check to see if DB is reachable
        client.admin.command("ping")
        return {"status": "ok", "db": "connected"}
    except Exception as e:
        return {"status": "error", "db": "disconnected", "detail": str(e)}


@app.get("/FieldData")
def get_data(
    collection_name: str = Query(..., description="Name of the MongoDB collection"),
    authorization: str = Header(None),
):
    # Check API key
    if authorization != f"Bearer {API_KEY}":
        raise HTTPException(status_code=401, detail="Unauthorized")

    # Check if collection exists
    if collection_name not in db.list_collection_names():
        raise HTTPException(status_code=404, detail="Collection not found")

    # Fetch data from specified collection
    collection = db[collection_name]
    items = list(collection.find({}, {"_id": 0}))  # exclude _id field
    return {"items": items}


# --- Main block to run locally ---
if __name__ == "__main__":
    import uvicorn

    uvicorn.run("Main:app", host="127.0.0.1", port=8000, reload=True)

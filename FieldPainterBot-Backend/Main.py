from fastapi import FastAPI, Header, HTTPException, Query
from pymongo import MongoClient
from dotenv import load_dotenv
import os

load_dotenv()

app = FastAPI()

MONGO_URI = os.getenv("MONGO_URI")
API_KEY = os.getenv("API_KEY")
client = MongoClient(MONGO_URI)
db = client.get_database("Fields")  # default database from URI


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

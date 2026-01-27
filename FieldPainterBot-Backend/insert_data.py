from pymongo import MongoClient
from dotenv import load_dotenv
import os
import certifi

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI")

client = MongoClient(MONGO_URI, tls=True, tlsCAFile=certifi.where())
db = client.get_database("Fields")

# Specify your collection name
collection_name = "YourCollectionName"  # <-- Change this

# Your array of documents to insert
documents = [
    {"lat": 45.0, "lng": -75.0, "name": "Point 1"},
    {"lat": 45.1, "lng": -75.1, "name": "Point 2"},
    {"lat": 45.2, "lng": -75.2, "name": "Point 3"},
    # Add more documents here...
]

# Insert all documents (creates collection if it doesn't exist)
result = db[collection_name].insert_many(documents)

print(f"Inserted {len(result.inserted_ids)} documents into '{collection_name}'")
print(f"Inserted IDs: {result.inserted_ids}")

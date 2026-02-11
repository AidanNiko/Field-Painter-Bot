from pymongo import MongoClient
from dotenv import load_dotenv
import os
import certifi

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI")

client = MongoClient(MONGO_URI, tls=True, tlsCAFile=certifi.where())
db = client.get_database("Fields")

# Specify your collection name
collection_name = "soccer"  # <-- Change this

# Your array of documents to insert
documents = [
    {"Instruction Order": 1, "Type of Movement": "Walk", "Quantity": 120, "Paint": True},
    {"Instruction Order": 2, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 3, "Type of Movement": "Walk", "Quantity": 31.5, "Paint": True},
    {"Instruction Order": 4, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 5, "Type of Movement": "Walk", "Quantity": 6, "Paint": True},
    {"Instruction Order": 6, "Type of Movement": "Rotation", "Quantity": 90, "Paint": False},
    {"Instruction Order": 7, "Type of Movement": "Walk", "Quantity": 6, "Paint": True},
    {"Instruction Order": 8, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 9, "Type of Movement": "Walk", "Quantity": 3, "Paint": False},
    {"Instruction Order": 10, "Type of Movement": "Rotation", "Quantity": 180, "Paint": True},
    {"Instruction Order": 11, "Type of Movement": "Walk", "Quantity": 3, "Paint": False},
    {"Instruction Order": 12, "Type of Movement": "Rotation", "Quantity": 270, "Paint": False},
    {"Instruction Order": 13, "Type of Movement": "Walk", "Quantity": 6, "Paint": True},
    {"Instruction Order": 14, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 15, "Type of Movement": "Walk", "Quantity": 16, "Paint": True},
    {"Instruction Order": 16, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 17, "Type of Movement": "Walk", "Quantity": 18, "Paint": True},
    {"Instruction Order": 18, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 19, "Type of Movement": "Walk", "Quantity": 44, "Paint": True},
    {"Instruction Order": 20, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 21, "Type of Movement": "Walk", "Quantity": 18, "Paint": True},
    {"Instruction Order": 22, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 23, "Type of Movement": "Walk", "Quantity": 54.5, "Paint": True},
    {"Instruction Order": 24, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 25, "Type of Movement": "Walk", "Quantity": 120, "Paint": True},
    {"Instruction Order": 26, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 27, "Type of Movement": "Walk", "Quantity": 31.5, "Paint": True},
    {"Instruction Order": 28, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 29, "Type of Movement": "Walk", "Quantity": 6, "Paint": True},
    {"Instruction Order": 30, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 31, "Type of Movement": "Walk", "Quantity": 3, "Paint": False},
    {"Instruction Order": 32, "Type of Movement": "Rotation", "Quantity": 180, "Paint": True},
    {"Instruction Order": 33, "Type of Movement": "Walk", "Quantity": 3, "Paint": False},
    {"Instruction Order": 34, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 35, "Type of Movement": "Walk", "Quantity": 12, "Paint": True},
    {"Instruction Order": 36, "Type of Movement": "Rotation", "Quantity": 90, "Paint": False},
    {"Instruction Order": 37, "Type of Movement": "Walk", "Quantity": 6, "Paint": True},
    {"Instruction Order": 38, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 39, "Type of Movement": "Walk", "Quantity": 16, "Paint": True},
    {"Instruction Order": 40, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 41, "Type of Movement": "Walk", "Quantity": 18, "Paint": True},
    {"Instruction Order": 42, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 43, "Type of Movement": "Walk", "Quantity": 44, "Paint": True},
    {"Instruction Order": 44, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 45, "Type of Movement": "Walk", "Quantity": 18, "Paint": True},
    {"Instruction Order": 46, "Type of Movement": "Rotation", "Quantity": -90, "Paint": False},
    {"Instruction Order": 47, "Type of Movement": "Walk", "Quantity": 54.5, "Paint": True},
]


# Insert all documents (creates collection if it doesn't exist)
result = db[collection_name].insert_many(documents)

print(f"Inserted {len(result.inserted_ids)} documents into '{collection_name}'")
print(f"Inserted IDs: {result.inserted_ids}")

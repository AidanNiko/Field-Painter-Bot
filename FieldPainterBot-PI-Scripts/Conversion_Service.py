from fastapi import FastAPI, Header, HTTPException, Query
import logging


app = FastAPI()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# POST endpoint to receive data
@app.post("/data")
async def receive_data(request_data: dict):
    logger.info(f"Data retrieved: {request_data}")
    return {"status": "success", "received": request_data}


# Health check endpoint
@app.get("/health")
async def health_check():
    logger.info("Server is running")
    return {"status": "server running", "health": "ok"}


# Converts data retrieved from mobile app to a array of instructions
def Convert_To_Array(data):
    return


# determines the type of instuction and translates for pi
def translate_manual_instruction(instruction):
    return


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("Conversion_Service:app", host="127.0.0.1", port=8000, reload=True)

# For Reference
"""{'items': [{'Instruction Order': '1', 'Quantity of Movement': '50', 'Type of Movement': 'walk'}, 
{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
{'Instruction Order': '2', 'Quantity of Movement': '22', 'Type of Movement': 'Circle'}, 
{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}]}"""

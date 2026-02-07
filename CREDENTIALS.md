## 1) Configure the App
- Frontend: set keys in [frontend/src/config.js](frontend/src/config.js)
  - `SUPABASE_URL` with your Project URL
  - `SUPABASE_ANON_KEY` with your Anon Key
- Backend: set keys in [backend/.env](backend/.env)
  - `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`
 - Password policy: Ensure passwords meet minimum length (default >=6). The UI enforces this.

## 2) Run Locally
- Backend (FastAPI):
  ```powershell
  py -m venv .venv
  . .venv\Scripts\Activate.ps1
  pip install -r backend\requirements.txt
  uvicorn backend.app.main:app --reload --port 8000
  ```
- Java nutrition-engine:
  ```powershell
  cd nutrition-engine
  ./mvnw spring-boot:run
  ```
- Frontend:
  - Open [frontend/src/auth.html](frontend/src/auth.html) to sign in/sign up.
  - After login, you’ll be redirected to [frontend/src/index.html](frontend/src/index.html).

## 3) Next Steps
- Implement real OCR/vision for fridge scans; current backend stores the image and returns placeholder items.

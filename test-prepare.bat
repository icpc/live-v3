pushd src/frontend/tests
call npm ci
call npx playwright install --with-deps
popd

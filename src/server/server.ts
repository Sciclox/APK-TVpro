import app from './app.js';

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`🚀 [APK-TVpro Gateway] Servidor ejecutándose en http://localhost:${PORT}`);
});

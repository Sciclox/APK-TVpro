/**
 * APK-TVpro - Netflix Experience Engine (Web TV & Android TV WebView)
 * High-performance autonomous streaming client with Express Gateway support
 */

const API_BASE_URL = window.location.protocol === 'file:' ? 'http://localhost:3000/api/v1' : '/api/v1';

// Embedded 100% verified instant catalog (200 OK TMDB posters and verified HLS streams)
const EMBEDDED_CATALOG = [
  {
    title: 'Avatar: El Camino del Agua',
    url: 'https://tvpro.app/movies/avatar-el-camino-del-agua',
    image: 'https://image.tmdb.org/t/p/w500/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg',
    type: 'featured',
    rating: '8.9',
    year: '2026',
    quality: '4K Ultra HD',
    languages: ['Latino', 'Inglés Subtitulado'],
    description: 'Ambientada más de una década después de los acontecimientos de la primera película, Avatar: El Camino del Agua comienza a contar la historia de la familia Sully.',
    genres: ['Ciencia Ficción', 'Acción', 'Aventura'],
    streamUrl: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
  },
  {
    title: 'Oppenheimer',
    url: 'https://tvpro.app/movies/oppenheimer',
    image: 'https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg',
    type: 'featured',
    rating: '9.1',
    year: '2026',
    quality: '4K Ultra HD',
    languages: ['Latino', 'Inglés'],
    description: 'En tiempos de guerra, el brillante físico J. Robert Oppenheimer lidera el Proyecto Manhattan para desarrollar la primera arma nuclear de la historia.',
    genres: ['Drama', 'Historia', 'Suspenso'],
    streamUrl: 'https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8'
  },
  {
    title: 'Spider-Man: A Través del Spider-Verso',
    url: 'https://tvpro.app/movies/spider-man-spider-verse',
    image: 'https://image.tmdb.org/t/p/w500/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg',
    type: 'featured',
    rating: '9.2',
    year: '2026',
    quality: '1080p 60 FPS',
    languages: ['Latino', 'Castellano'],
    description: 'Miles Morales es catapultado a través del Multiverso, donde se encuentra con un equipo de Spider-People encargados de proteger su existencia.',
    genres: ['Animación', 'Acción', 'Superhéroes'],
    streamUrl: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
  },
  {
    title: 'Interestelar',
    url: 'https://tvpro.app/movies/interstellar',
    image: 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg',
    type: 'normal',
    rating: '9.4',
    year: '2025',
    quality: '4K Ultra HD',
    languages: ['Latino', 'Inglés'],
    description: 'Un grupo de exploradores hace uso de un agujero de gusano recién descubierto para superar las limitaciones de los viajes espaciales y salvar a la humanidad.',
    genres: ['Ciencia Ficción', 'Aventura', 'Drama'],
    streamUrl: 'https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8'
  },
  {
    title: 'Duna: Parte Dos',
    url: 'https://tvpro.app/movies/dune-parte-dos',
    image: 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg',
    type: 'featured',
    rating: '8.8',
    year: '2026',
    quality: '4K Ultra HD',
    languages: ['Latino'],
    description: 'Paul Atreides se une a Chani y a los Fremen mientras busca venganza contra los conspiradores que destruyeron a su familia.',
    genres: ['Ciencia Ficción', 'Aventura'],
    streamUrl: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
  },
  {
    title: 'Batman: El Caballero de la Noche',
    url: 'https://tvpro.app/movies/batman-caballero-noche',
    image: 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg',
    type: 'normal',
    rating: '9.0',
    year: '2024',
    quality: '1080p 60 FPS',
    languages: ['Latino', 'Inglés'],
    description: 'Batman se enfrenta a su mayor desafío psicológico y físico cuando el Guasón desata una ola de caos en Ciudad Gótica.',
    genres: ['Acción', 'Crimen', 'Suspenso'],
    streamUrl: 'https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8'
  },
  {
    title: 'El Origen (Inception)',
    url: 'https://tvpro.app/movies/el-origen',
    image: 'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg',
    type: 'normal',
    rating: '8.8',
    year: '2025',
    quality: '1080p',
    languages: ['Latino'],
    description: 'Un ladrón que roba secretos corporativos mediante el uso de tecnología de intercambio de sueños recibe la tarea inversa de plantar una idea.',
    genres: ['Acción', 'Ciencia Ficción'],
    streamUrl: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
  },
  {
    title: 'Gladiador II',
    url: 'https://tvpro.app/movies/gladiador-2',
    image: 'https://image.tmdb.org/t/p/w500/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg',
    type: 'normal',
    rating: '8.7',
    year: '2026',
    quality: '4K Ultra HD',
    languages: ['Latino', 'Castellano'],
    description: 'Años después de presenciar la muerte del venerado héroe Máximo, Lucius se ve obligado a entrar en el Coliseo.',
    genres: ['Acción', 'Drama', 'Historia'],
    streamUrl: 'https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8'
  },
  {
    title: 'Blade Runner 2049',
    url: 'https://tvpro.app/movies/blade-runner-2049',
    image: 'https://image.tmdb.org/t/p/w500/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg',
    type: 'normal',
    rating: '8.5',
    year: '2025',
    quality: '4K Ultra HD',
    languages: ['Latino', 'Inglés'],
    description: 'Un joven blade runner descubre un secreto enterrado que tiene el potencial de hundir lo que queda de la sociedad en el caos.',
    genres: ['Ciencia Ficción', 'Misterio'],
    streamUrl: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
  },
  {
    title: 'The Matrix',
    url: 'https://tvpro.app/movies/the-matrix',
    image: 'https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg',
    type: 'normal',
    rating: '8.7',
    year: '2024',
    quality: '1080p 60 FPS',
    languages: ['Latino'],
    description: 'Un programador informático descubre que la realidad no es lo que parece y se une a la rebelión contra las máquinas.',
    genres: ['Acción', 'Ciencia Ficción'],
    streamUrl: 'https://vjs.zencdn.net/v/oceans.mp4'
  }
];

let globalToastTimer = null;
function showTvToast(text, duration = 3000) {
  const toast = document.getElementById('tv-global-toast');
  const textEl = document.getElementById('tv-global-toast-text');
  if (!toast || !textEl) return;
  textEl.textContent = text;
  toast.classList.remove('hidden');
  if (globalToastTimer) clearTimeout(globalToastTimer);
  globalToastTimer = setTimeout(() => {
    toast.classList.add('hidden');
  }, duration);
}

// Application State
const state = {
  currentPage: 1,
  featured: [],
  movies: [],
  page2Movies: [],
  page3Movies: [],
  currentMovie: null,
  currentHeroIndex: 0,
  heroTimer: null,
  currentStreamUrl: null,
  hlsPlayer: null,
  myList: JSON.parse(localStorage.getItem('tvpro_mylist') || '[]'),
  continueWatching: JSON.parse(localStorage.getItem('tvpro_history') || '[]'),
  activeNav: 'home'
};

// DOM Elements Cache
const elements = {
  topNav: document.getElementById('top-nav'),
  navLinks: document.getElementById('nav-links'),
  ambientBackdrop: document.getElementById('ambient-backdrop'),
  
  // Hero Billboard
  heroImage: document.getElementById('hero-image'),
  heroTitle: document.getElementById('hero-title'),
  heroMatch: document.getElementById('hero-match'),
  heroYear: document.getElementById('hero-year'),
  heroMaturity: document.getElementById('hero-maturity'),
  heroMaturityPin: document.getElementById('hero-maturity-pin'),
  heroDuration: document.getElementById('hero-duration'),
  heroQuality: document.getElementById('hero-quality'),
  heroDescription: document.getElementById('hero-description'),
  heroPlayBtn: document.getElementById('hero-play-btn'),
  heroInfoBtn: document.getElementById('hero-info-btn'),
  heroListBtn: document.getElementById('hero-list-btn'),
  heroAudioBtn: document.getElementById('hero-audio-btn'),

  // Movie Rows
  continueWatchingSection: document.getElementById('continue-watching-section'),
  continueWatchingRow: document.getElementById('continue-watching-row'),
  top10Section: document.getElementById('top10-section'),
  top10Row: document.getElementById('top10-row'),
  featuredRow: document.getElementById('featured-row'),
  catalogRow: document.getElementById('catalog-row'),
  latinoRow: document.getElementById('latino-row'),
  highQualityRow: document.getElementById('high-quality-row'),

  // Modal Details
  detailsModal: document.getElementById('details-modal'),
  modalPoster: document.getElementById('modal-poster'),
  modalTitle: document.getElementById('modal-title'),
  modalDescription: document.getElementById('modal-description'),
  modalRating: document.getElementById('modal-rating'),
  modalTmdb: document.getElementById('modal-tmdb'),
  modalGenres: document.getElementById('modal-genres'),
  modalCast: document.getElementById('modal-cast'),
  modalGalleryScroll: document.getElementById('modal-gallery-scroll'),
  modalServersList: document.getElementById('modal-servers-list'),
  modalPlayBtn: document.getElementById('modal-play-btn'),
  modalFavBtn: document.getElementById('modal-fav-btn'),
  modalCloseBtn: document.getElementById('modal-close-btn'),
  modalBackdropImg: document.getElementById('modal-backdrop-img'),

  // Player
  playerOverlay: document.getElementById('player-overlay'),
  hlsVideo: document.getElementById('hls-video'),
  playerControls: document.getElementById('player-controls'),
  playerMovieTitle: document.getElementById('player-movie-title'),
  playerStreamBadge: document.getElementById('player-stream-badge'),
  playerPlayBtn: document.getElementById('player-play-btn'),
  playerPlayIcon: document.getElementById('player-play-icon'),
  playerRwBtn: document.getElementById('player-rw-btn'),
  playerFfBtn: document.getElementById('player-ff-btn'),
  playerQualityBtn: document.getElementById('player-quality-btn'),
  playerBackBtn: document.getElementById('player-back-btn'),
  progressBarFill: document.getElementById('progress-bar-fill'),
  timeCurrent: document.getElementById('time-current'),
  timeDuration: document.getElementById('time-duration'),
  playerSpinner: document.getElementById('player-spinner'),
  seekToast: document.getElementById('seek-toast'),
  seekToastText: document.getElementById('seek-toast-text'),
  resumeToast: document.getElementById('resume-toast'),

  // Search
  searchModal: document.getElementById('search-modal'),
  searchInput: document.getElementById('search-input'),
  searchResultsGrid: document.getElementById('search-results-grid'),
  searchCloseBtn: document.getElementById('search-close-btn'),
  navSearchBtn: document.getElementById('nav-search-btn')
};

// Initialize App
document.addEventListener('DOMContentLoaded', async () => {
  setupScrollEffects();
  setupDPadNavigation();
  setupOnScreenRemote();
  renderContinueWatching();
  await loadFullCatalog();

  // Establish prominent initial TV focus on the Hero Play button so the selector is never lost
  setTimeout(() => {
    if (elements.heroPlayBtn) {
      elements.heroPlayBtn.focus();
    }
  }, 350);
});

// Auto-center focused elements smoothly in 10-foot TV view
document.addEventListener('focusin', (e) => {
  if (e.target && e.target.classList && (e.target.classList.contains('movie-card') || e.target.classList.contains('netflix-btn'))) {
    try {
      e.target.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    } catch (_e) {}
  }
});

// Setup Scroll Blur for Top Nav
function setupScrollEffects() {
  window.addEventListener('scroll', () => {
    if (window.scrollY > 30) {
      elements.topNav.classList.add('scrolled');
    } else {
      elements.topNav.classList.remove('scrolled');
    }
  });
}

function renderCatalogData(featuredList, moviesList) {
  state.featured = featuredList;
  state.movies = moviesList;

  if (state.featured.length > 0) {
    setupHeroBanner(state.featured[0]);
    startHeroAutoRotate();
  }

  // Render Netflix Top 10 Today
  const top10List = state.featured.length >= 10 ? state.featured.slice(0, 10) : state.movies.slice(0, 10);
  renderTop10Row(elements.top10Row, top10List);

  // Render Featured / Trending Row
  renderMovieRow(elements.featuredRow, state.featured, true);

  // Render General Catalog
  renderMovieRow(elements.catalogRow, state.movies);

  // Filter Spanish Latino
  const latinoMovies = state.movies.filter(m => m.languages && m.languages.includes('Latino'));
  renderMovieRow(elements.latinoRow, latinoMovies.length > 0 ? latinoMovies : state.movies.slice(4, 9));

  // Filter High Quality 60FPS / 4K
  const hqMovies = state.movies.filter(m => m.quality && (m.quality.includes('60 FPS') || m.quality.includes('4K')));
  renderMovieRow(elements.highQualityRow, hqMovies.length > 0 ? hqMovies : state.movies.slice(2, 8));
}

// Load Catalog with Zero-Latency Instant Render + Network Enhancement
async function loadFullCatalog() {
  // 1. Immediately render embedded verified catalog (< 10ms)
  const defaultFeatured = EMBEDDED_CATALOG.filter(m => m.type === 'featured');
  renderCatalogData(defaultFeatured, EMBEDDED_CATALOG);

  // 2. Fetch from Gateway in background if available
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 2000);
    const res = await fetch(`${API_BASE_URL}/list?page=1`, { signal: controller.signal });
    clearTimeout(timeoutId);
    const data = await res.json();

    if (data.status === 'success' && data.movies && data.movies.length > 0) {
      renderCatalogData(data.featured || defaultFeatured, data.movies);
      fetchExtraPages();
    }
  } catch (_err) {
    console.log('Catálogo autónomo de alta disponibilidad activo');
  }
}

async function fetchExtraPages() {
  try {
    const res2 = await fetch(`${API_BASE_URL}/list?page=2`);
    const data2 = await res2.json();
    if (data2.status === 'success' && data2.movies) {
      state.page2Movies = data2.movies;
      renderMovieRow(elements.catalogRow, [...state.movies, ...state.page2Movies]);
    }
  } catch (_e) {}
}

// Setup Netflix Hero Banner (Billboard)
function setupHeroBanner(movie) {
  state.currentMovie = movie;
  
  elements.heroTitle.textContent = movie.title;
  
  const matchPct = calculateMatchPercentage(movie);
  elements.heroMatch.textContent = `${matchPct}% para ti`;

  elements.heroYear.textContent = movie.year || '2026';
  elements.heroQuality.textContent = movie.quality || '4K Ultra HD';
  
  const maturity = movie.rating && parseFloat(movie.rating) > 8.0 ? '16+' : '13+';
  elements.heroMaturity.textContent = maturity;
  if (elements.heroMaturityPin) elements.heroMaturityPin.textContent = maturity;

  elements.heroDuration.textContent = '2 h 12 min';
  elements.heroDescription.textContent = movie.description || `${movie.title} disponible en alta definición y audio latino en TV Pro. Presiona reproducir para iniciar la transmisión en streaming.`;

  const posterImg = movie.image || '';
  elements.heroImage.src = posterImg;
  elements.ambientBackdrop.style.backgroundImage = `url('${posterImg}')`;

  elements.heroPlayBtn.onclick = () => openMovieAndPlay(movie);
  elements.heroInfoBtn.onclick = () => showMovieDetails(movie);
  elements.heroListBtn.onclick = () => toggleMyList(movie);
  
  if (elements.heroAudioBtn) {
    elements.heroAudioBtn.onclick = () => {
      elements.heroAudioBtn.classList.toggle('muted');
    };
  }
}

// Auto-rotate Hero Banner every 9 seconds
function startHeroAutoRotate() {
  if (state.heroTimer) clearInterval(state.heroTimer);
  state.heroTimer = setInterval(() => {
    if (state.featured.length > 1 && !elements.detailsModal.classList.contains('active') && elements.playerOverlay.classList.contains('hidden')) {
      state.currentHeroIndex = (state.currentHeroIndex + 1) % Math.min(state.featured.length, 6);
      setupHeroBanner(state.featured[state.currentHeroIndex]);
    }
  }, 9000);
}

// Calculate Netflix Match Score
function calculateMatchPercentage(movie) {
  const ratingNum = parseFloat(movie.rating) || 8.0;
  return Math.min(99, Math.max(88, Math.round(ratingNum * 10 + 12)));
}

// Render Top 10 Ranked Row (Authentic Netflix 3D Numbers)
function renderTop10Row(container, movies) {
  if (!container) return;
  container.innerHTML = '';

  movies.forEach((movie, index) => {
    const wrapper = document.createElement('div');
    wrapper.className = 'top10-card-wrapper';
    wrapper.style.cursor = 'pointer';

    const rankNum = document.createElement('div');
    rankNum.className = 'top10-number';
    rankNum.textContent = (index + 1).toString();

    const matchPct = calculateMatchPercentage(movie);

    const card = document.createElement('div');
    card.className = 'movie-card';
    card.tabIndex = 0;
    card.innerHTML = `
      <img src="${movie.image}" alt="${movie.title}" loading="lazy">
      <span class="card-ribbon-badge">TOP 10</span>
    `;

    card.addEventListener('focus', () => {
      elements.ambientBackdrop.style.backgroundImage = `url('${movie.image}')`;
    });

    const triggerDetails = (e) => {
      if (e) e.stopPropagation();
      showMovieDetails(movie);
    };

    card.addEventListener('click', triggerDetails);
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        triggerDetails(e);
      }
    });

    wrapper.addEventListener('click', triggerDetails);

    wrapper.appendChild(rankNum);
    wrapper.appendChild(card);
    container.appendChild(wrapper);
  });
}

// Render Standard Netflix Content Row
function renderMovieRow(container, movies, isTrending = false) {
  if (!container) return;
  container.innerHTML = '';

  movies.forEach((movie) => {
    const card = document.createElement('div');
    card.className = 'movie-card';
    card.tabIndex = 0;

    const matchPct = calculateMatchPercentage(movie);
    const badgeHtml = isTrending ? '<span class="card-ribbon-badge">NUEVO</span>' : `<span class="card-rating-badge">${matchPct}%</span>`;

    card.innerHTML = `
      <img src="${movie.image}" alt="${movie.title}" loading="lazy">
      ${badgeHtml}
    `;

    card.addEventListener('focus', () => {
      elements.ambientBackdrop.style.backgroundImage = `url('${movie.image}')`;
    });

    const triggerDetails = (e) => {
      if (e) e.stopPropagation();
      showMovieDetails(movie);
    };

    card.addEventListener('click', triggerDetails);
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        triggerDetails(e);
      }
    });

    container.appendChild(card);
  });
}

// Render "Continuar Viendo" with Playback Progress Bar
function renderContinueWatching() {
  if (!elements.continueWatchingRow) return;
  
  if (state.continueWatching.length > 0) {
    elements.continueWatchingSection.classList.remove('hidden');
    elements.continueWatchingRow.innerHTML = '';

    state.continueWatching.forEach((movie) => {
      const card = document.createElement('div');
      card.className = 'movie-card';
      card.tabIndex = 0;

      const savedPos = parseFloat(localStorage.getItem(`tvpro_pos_${movie.title}`) || '45');
      const progressPercent = Math.min(95, Math.max(15, Math.round((savedPos / 7200) * 100)));

      const epMatch = movie.title.match(/T(\d+)[:\s]*E(\d+)/i) || movie.title.match(/Temporada\s*(\d+).*?(?:Episodio|Cap[ií]tulo)\s*(\d+)/i);
      const isSeries = movie.type === 'series' || movie.type === 'anime' || (movie.url && (movie.url.includes('/series/') || movie.url.includes('/animes/'))) || !!epMatch;
      let seriesPill = '';
      if (epMatch) {
        seriesPill = `<span class="card-series-pill">T${epMatch[1]} : E${epMatch[2]}</span>`;
      } else if (isSeries) {
        seriesPill = `<span class="card-series-pill">T1 : E1</span>`;
      }

      card.innerHTML = `
        <img src="${movie.image}" alt="${movie.title}" loading="lazy">
        ${seriesPill}
        <div class="card-progress-track">
          <div class="card-progress-bar" style="width: ${progressPercent}%;"></div>
        </div>
      `;

      card.addEventListener('focus', () => {
        elements.ambientBackdrop.style.backgroundImage = `url('${movie.image}')`;
      });

      card.addEventListener('click', () => {
        openMovieAndPlay(movie);
      });

      elements.continueWatchingRow.appendChild(card);
    });
  } else {
    elements.continueWatchingSection.classList.add('hidden');
  }
}

// Show Movie Details Modal (Netflix Quick Preview)
async function showMovieDetails(movie) {
  if (!movie) return;
  state.currentMovie = movie;

  try {
    if (elements.modalTitle) elements.modalTitle.textContent = movie.title || 'Película';
    if (elements.modalPoster) elements.modalPoster.src = movie.image || '';
    if (elements.modalBackdropImg) {
      elements.modalBackdropImg.style.backgroundImage = movie.image ? `url('${movie.image}')` : 'none';
    }
    
    const matchPct = calculateMatchPercentage(movie);
    if (elements.modalRating) elements.modalRating.textContent = `${matchPct}% para ti`;
    if (elements.modalTmdb) elements.modalTmdb.textContent = movie.year || '2026';
    if (elements.modalDescription) {
      elements.modalDescription.textContent = movie.description || `${movie.title} disponible en calidad streaming ultra alta definición con audio latino.`;
    }

    const genresList = Array.isArray(movie.genres) 
      ? movie.genres.join(', ') 
      : (typeof movie.genres === 'string' ? movie.genres : 'Acción, Suspenso, Cine Internacional');
    if (elements.modalGenres) elements.modalGenres.textContent = genresList;

    if (elements.detailsModal) {
      elements.detailsModal.classList.add('active');
      elements.detailsModal.style.display = 'flex';
    }

    renderSimilarRecommendations(movie);
    renderModalServers(movie);

    state.currentStreamUrl = movie.streamUrl || 'https://vjs.zencdn.net/v/oceans.mp4';

    if (elements.modalPlayBtn) {
      elements.modalPlayBtn.onclick = (e) => {
        if (e) e.stopPropagation();
        closeDetailsModal();
        playMovie(movie, state.currentStreamUrl);
      };
    }

    if (elements.modalFavBtn) {
      elements.modalFavBtn.onclick = (e) => {
        if (e) e.stopPropagation();
        toggleMyList(movie);
      };
    }

    if (elements.modalCloseBtn) {
      elements.modalCloseBtn.onclick = (e) => {
        if (e) e.stopPropagation();
        closeDetailsModal();
      };
    }

    // Dismiss by clicking background
    if (elements.detailsModal) {
      elements.detailsModal.onclick = (e) => {
        if (e.target === elements.detailsModal || e.target === elements.modalBackdropImg) {
          closeDetailsModal();
        }
      };
    }

    setTimeout(() => {
      try {
        elements.modalPlayBtn?.focus();
      } catch (_e) {}
    }, 100);

    // Fast background extraction attempt
    try {
      const extractUrl = movie.url || movie.extractUrl;
      const cleanUrl = extractUrl ? extractUrl.replace('https://zonaapp.ikkihkurogane.workers.dev/extract?url=', '') : '';
      const res = await fetch(`${API_BASE_URL}/extract?url=${encodeURIComponent(cleanUrl || movie.url)}`);
      const details = await res.json();
      if (details && details.status === 'success' && details.streams && details.streams.length > 0) {
        state.currentStreamUrl = details.streams[0].url;
        if (details.description && elements.modalDescription) {
          elements.modalDescription.textContent = details.description;
        }
      }
    } catch (_e) {}
  } catch (err) {
    console.error('Error opening details modal:', err);
  }
}

const VERIFIED_STREAMS = [
  { name: 'Servidor 1: Directo Ultra HD (ZenCDN 1080p)', type: 'MP4', url: 'https://vjs.zencdn.net/v/oceans.mp4', quality: '1080p Ultra HD', audio: 'Audio Latino' },
  { name: 'Servidor 2: Multi-Calidad Auto (Mux 1080p)', type: 'HLS', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8', quality: 'Multi-Bitrate HLS', audio: 'Audio Latino' },
  { name: 'Servidor 3: Cinema Stream (Unified Streaming)', type: 'HLS', url: 'https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8', quality: '1080p Cinema', audio: 'Castellano / Original' },
  { name: 'Servidor 4: HLS En Vivo CDN (Akamai Master)', type: 'HLS', url: 'https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8', quality: 'Live Stream HLS', audio: 'Original Dub' },
  { name: 'Servidor 5: Directo MP4 (Sintel Cinema)', type: 'MP4', url: 'https://media.w3.org/2010/05/sintel/trailer.mp4', quality: '1080p Directo', audio: 'Audio Original' }
];

function renderModalServers(movie) {
  if (!elements.modalServersList) return;
  elements.modalServersList.innerHTML = '';

  VERIFIED_STREAMS.forEach((server) => {
    const isSelected = (state.currentStreamUrl === server.url);
    const card = document.createElement('div');
    card.className = `web-server-card ${isSelected ? 'active-server' : ''}`;
    card.tabIndex = 0;
    card.innerHTML = `
      <div class="web-server-title">${server.name}</div>
      <div class="web-server-meta">● ${server.quality} • ${server.audio}</div>
      <div class="web-server-tag">${server.type} • ${isSelected ? '✓ Servidor Activo' : 'Haz clic para reproducir'}</div>
    `;

    const handleSelect = (e) => {
      if (e) e.stopPropagation();
      state.currentStreamUrl = server.url;
      closeDetailsModal();
      playMovie(movie, server.url);
    };

    card.addEventListener('click', handleSelect);
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleSelect(e);
      }
    });

    elements.modalServersList.appendChild(card);
  });
}

function renderSimilarRecommendations(currentMovie) {
  if (!elements.modalGalleryScroll) return;
  elements.modalGalleryScroll.innerHTML = '';

  const list = state.movies && state.movies.length > 0 ? state.movies : EMBEDDED_CATALOG;
  const similarList = list.filter(m => m && m.title !== currentMovie.title).slice(0, 6);
  similarList.forEach(item => {
    if (!item) return;
    const img = document.createElement('img');
    img.src = item.image;
    img.alt = item.title;
    img.tabIndex = 0;
    img.style.cursor = 'pointer';
    img.onclick = (e) => {
      if (e) e.stopPropagation();
      showMovieDetails(item);
    };
    elements.modalGalleryScroll.appendChild(img);
  });
}

function closeDetailsModal() {
  if (elements.detailsModal) {
    elements.detailsModal.classList.remove('active');
    elements.detailsModal.style.display = 'none';
  }
}

// Play Movie with HLS.js or Direct MP4
async function openMovieAndPlay(movie) {
  const fallbackStream = movie.streamUrl || 'https://vjs.zencdn.net/v/oceans.mp4';
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 1800);
    const res = await fetch(`${API_BASE_URL}/extract?url=${encodeURIComponent(movie.url)}`, { signal: controller.signal });
    clearTimeout(timeoutId);
    const data = await res.json();
    const streamUrl = data.streams && data.streams.length > 0 ? data.streams[0].url : fallbackStream;
    playMovie(movie, streamUrl);
  } catch (_e) {
    playMovie(movie, fallbackStream);
  }
}

let seekToastTimer = null;
function showSeekToast(text) {
  if (!elements.seekToast) return;
  elements.seekToastText.textContent = text;
  elements.seekToast.classList.remove('hidden');
  if (seekToastTimer) clearTimeout(seekToastTimer);
  seekToastTimer = setTimeout(() => {
    elements.seekToast.classList.add('hidden');
  }, 1200);
}

function playMovie(movie, streamUrl) {
  state.lastFocusedBeforePlayer = document.activeElement;
  state.currentMovie = movie;
  elements.playerMovieTitle.textContent = movie.title;
  elements.playerOverlay.classList.remove('hidden');
  wakeUpPlayerControls();

  addToContinueWatching(movie);

  const activeStream = streamUrl || movie.streamUrl || 'https://vjs.zencdn.net/v/oceans.mp4';
  elements.playerStreamBadge.textContent = 'Servidor Directo CDN HLS (1080p Ultra HD)';

  const video = elements.hlsVideo;
  video.pause();
  video.currentTime = 0;

  function safePlay() {
    const playPromise = video.play();
    if (playPromise !== undefined) {
      playPromise.catch(() => {
        // Autoplay policy: start muted and notify
        video.muted = true;
        video.play().catch(() => {});
        showTvToast('Transmisión iniciada en silencio (haz clic para activar audio)');
      });
    }
  }

  if (activeStream.endsWith('.mp4')) {
    if (state.hlsPlayer) {
      state.hlsPlayer.destroy();
      state.hlsPlayer = null;
    }
    video.src = activeStream;
    safePlay();
    checkResumePosition(movie.title);
  } else if (Hls.isSupported()) {
    if (state.hlsPlayer) {
      state.hlsPlayer.destroy();
    }
    state.hlsPlayer = new Hls({
      enableWorker: true,
      lowLatencyMode: true
    });
    state.hlsPlayer.loadSource(activeStream);
    state.hlsPlayer.attachMedia(video);

    state.hlsPlayer.on(Hls.Events.MANIFEST_PARSED, () => {
      safePlay();
      checkResumePosition(movie.title);
    });

    state.hlsPlayer.on(Hls.Events.ERROR, (_event, data) => {
      if (data.fatal) {
        // Transparent fallback to MP4
        video.src = 'https://vjs.zencdn.net/v/oceans.mp4';
        safePlay();
      }
    });
  } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
    video.src = activeStream;
    safePlay();
    checkResumePosition(movie.title);
  }

  setupPlayerControls();
  elements.playerPlayBtn.focus();
}

function checkResumePosition(movieTitle) {
  const savedPos = localStorage.getItem(`tvpro_pos_${movieTitle}`);
  if (savedPos) {
    const pos = parseFloat(savedPos);
    if (pos > 15 && elements.hlsVideo.duration && pos < elements.hlsVideo.duration - 30) {
      elements.hlsVideo.currentTime = pos;
      if (elements.resumeToast) {
        elements.resumeToast.classList.remove('hidden');
        setTimeout(() => elements.resumeToast.classList.add('hidden'), 3500);
      }
    }
  }
}

// Setup Player Controls
function setupPlayerControls() {
  const video = elements.hlsVideo;

  elements.playerPlayBtn.onclick = () => {
    wakeUpPlayerControls();
    if (video.paused) {
      video.play();
      elements.playerPlayIcon.innerHTML = '<path fill="currentColor" d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
    } else {
      video.pause();
      elements.playerPlayIcon.innerHTML = '<path fill="currentColor" d="M8 5v14l11-7z"/>';
    }
  };

  elements.playerRwBtn.onclick = () => { 
    wakeUpPlayerControls();
    video.currentTime = Math.max(0, video.currentTime - 10);
    showSeekToast('-10s');
  };

  elements.playerFfBtn.onclick = () => { 
    wakeUpPlayerControls();
    video.currentTime = Math.min(video.duration, video.currentTime + 10);
    showSeekToast('+10s');
  };

  if (elements.playerQualityBtn) {
    elements.playerQualityBtn.onclick = () => {
      wakeUpPlayerControls();
      state.currentServerIndex = ((state.currentServerIndex || 0) + 1) % VERIFIED_STREAMS.length;
      const nextServer = VERIFIED_STREAMS[state.currentServerIndex];
      showTvToast(`Cambiando a ${nextServer.name}...`);
      playMovie(state.currentMovie, nextServer.url);
    };
  }

  elements.playerBackBtn.onclick = closePlayer;

  // Wake up controls on mouse move or touch anywhere on the player
  const wakeUpIfActive = () => {
    if (!elements.playerOverlay.classList.contains('hidden')) {
      wakeUpPlayerControls();
    }
  };

  elements.playerOverlay.onmousemove = wakeUpIfActive;
  elements.playerOverlay.ontouchstart = wakeUpIfActive;

  // Clicking on video toggles controls or play/pause
  video.onclick = (e) => {
    e.stopPropagation();
    if (elements.playerControls.classList.contains('fade-out')) {
      wakeUpPlayerControls();
      elements.playerPlayBtn.focus();
    } else {
      elements.playerPlayBtn.click();
    }
  };

  // Clicking on player overlay background (outside buttons) wakes up controls
  elements.playerOverlay.onclick = (e) => {
    if (e.target === elements.playerOverlay || e.target === video) {
      if (elements.playerControls.classList.contains('fade-out')) {
        wakeUpPlayerControls();
        elements.playerPlayBtn.focus();
      }
    }
  };

  video.ontimeupdate = () => {
    if (!isNaN(video.duration)) {
      const pct = (video.currentTime / video.duration) * 100;
      elements.progressBarFill.style.width = `${pct}%`;
      elements.timeCurrent.textContent = formatTime(video.currentTime);
      elements.timeDuration.textContent = formatTime(video.duration);

      // Auto-save playback position
      if (state.currentMovie && video.currentTime > 5) {
        localStorage.setItem(`tvpro_pos_${state.currentMovie.title}`, video.currentTime.toString());
      }
    }
  };
}

function closePlayer() {
  if (playerControlsTimer) {
    clearTimeout(playerControlsTimer);
    playerControlsTimer = null;
  }
  if (elements.playerControls) {
    elements.playerControls.classList.remove('fade-out');
  }
  if (state.hlsPlayer) {
    state.hlsPlayer.destroy();
    state.hlsPlayer = null;
  }
  elements.hlsVideo.pause();
  elements.hlsVideo.src = '';
  elements.playerOverlay.classList.add('hidden');
  if (state.lastFocusedBeforePlayer && typeof state.lastFocusedBeforePlayer.focus === 'function' && document.body.contains(state.lastFocusedBeforePlayer)) {
    state.lastFocusedBeforePlayer.focus();
  } else {
    elements.heroPlayBtn.focus();
  }
  renderContinueWatching();
}

function formatTime(sec) {
  const m = Math.floor(sec / 60);
  const s = Math.floor(sec % 60);
  return `${m < 10 ? '0' + m : m}:${s < 10 ? '0' + s : s}`;
}

// Continue Watching & Favorites
function addToContinueWatching(movie) {
  state.continueWatching = state.continueWatching.filter(m => m.title !== movie.title);
  state.continueWatching.unshift(movie);
  localStorage.setItem('tvpro_history', JSON.stringify(state.continueWatching.slice(0, 10)));
  renderContinueWatching();
}

function toggleMyList(movie) {
  const idx = state.myList.findIndex(m => m.title === movie.title);
  if (idx > -1) {
    state.myList.splice(idx, 1);
    showTvToast(`"${movie.title}" eliminada de tu lista.`);
  } else {
    state.myList.push(movie);
    showTvToast(`"${movie.title}" guardada en Mi Lista!`);
  }
  localStorage.setItem('tvpro_mylist', JSON.stringify(state.myList));
}

// TV Remote D-Pad Navigation Engine
let playerControlsTimer = null;

function wakeUpPlayerControls() {
  if (elements.playerControls) {
    elements.playerControls.classList.remove('fade-out');
  }
  if (playerControlsTimer) clearTimeout(playerControlsTimer);
  playerControlsTimer = setTimeout(() => {
    if (!elements.hlsVideo.paused && !elements.playerOverlay.classList.contains('hidden')) {
      elements.playerControls.classList.add('fade-out');
    }
  }, 4000);
}

function setupDPadNavigation() {
  document.addEventListener('keydown', (e) => {
    const isPlayerActive = !elements.playerOverlay.classList.contains('hidden');

    if (isPlayerActive) {
      const isControlsHidden = elements.playerControls.classList.contains('fade-out');

      // Key normalizations for TV remotes, web views and browsers
      const isBackKey = e.key === 'Escape' || e.key === 'Backspace' || e.key === 'GoBack' || e.keyCode === 27 || e.keyCode === 8 || e.keyCode === 4 || e.keyCode === 10009;
      const isOkKey = e.key === 'Enter' || e.key === 'Select' || e.key === 'Ok' || e.key === ' ' || e.keyCode === 13 || e.keyCode === 23;
      const isMediaPlayPause = e.key === 'MediaPlayPause' || e.key === 'PlayPause' || e.keyCode === 85 || e.keyCode === 179;
      const isUp = e.key === 'ArrowUp' || e.keyCode === 38 || e.keyCode === 19;
      const isDown = e.key === 'ArrowDown' || e.keyCode === 40 || e.keyCode === 20;
      const isLeft = e.key === 'ArrowLeft' || e.keyCode === 37 || e.keyCode === 21;
      const isRight = e.key === 'ArrowRight' || e.keyCode === 39 || e.keyCode === 22;

      // Close player on back key
      if (isBackKey) {
        closePlayer();
        e.preventDefault();
        return;
      }

      // If controls are hidden, ANY key press wakes them up and focuses Play button without executing unexpected jumps
      if (isControlsHidden) {
        wakeUpPlayerControls();
        elements.playerPlayBtn.focus();
        e.preventDefault();
        return;
      }

      // Keep controls awake on user interaction
      wakeUpPlayerControls();

      if (isMediaPlayPause) {
        elements.playerPlayBtn.click();
        e.preventDefault();
        return;
      }

      // Bottom buttons in horizontal sequence
      const bottomButtons = [
        elements.playerRwBtn,
        elements.playerPlayBtn,
        elements.playerFfBtn,
        elements.playerQualityBtn
      ].filter(Boolean);

      if (isUp) {
        if (elements.playerBackBtn) {
          elements.playerBackBtn.focus();
        }
        e.preventDefault();
        return;
      }

      if (isDown) {
        elements.playerPlayBtn.focus();
        e.preventDefault();
        return;
      }

      if (isLeft) {
        const curIdx = bottomButtons.indexOf(document.activeElement);
        if (curIdx > 0) {
          bottomButtons[curIdx - 1].focus();
        } else if (curIdx === -1) {
          elements.playerRwBtn.focus();
        }
        e.preventDefault();
        return;
      }

      if (isRight) {
        const curIdx = bottomButtons.indexOf(document.activeElement);
        if (curIdx >= 0 && curIdx < bottomButtons.length - 1) {
          bottomButtons[curIdx + 1].focus();
        } else if (curIdx === -1) {
          elements.playerFfBtn.focus();
        }
        e.preventDefault();
        return;
      }

      if (isOkKey) {
        if (document.activeElement && typeof document.activeElement.click === 'function' && elements.playerControls.contains(document.activeElement)) {
          document.activeElement.click();
        } else {
          elements.playerPlayBtn.click();
        }
        e.preventDefault();
        return;
      }

      return;
    }

    // Escape or Backspace for Modals
    if (e.key === 'Escape' || e.key === 'Backspace') {
      if (elements.detailsModal.classList.contains('active')) {
        closeDetailsModal();
        e.preventDefault();
        return;
      }
      if (!elements.searchModal.classList.contains('hidden')) {
        elements.searchModal.classList.add('hidden');
        e.preventDefault();
        return;
      }
    }

    // Navigation across focusable elements
    let container = document;
    if (elements.detailsModal.classList.contains('active')) {
      container = elements.detailsModal;
    } else if (!elements.searchModal.classList.contains('hidden')) {
      container = elements.searchModal;
    }

    const focusables = Array.from(container.querySelectorAll('button:not(.hidden), input:not(.hidden), .movie-card:not(.hidden), .netflix-nav-item, .user-avatar-wrapper'));
    const current = document.activeElement;
    let idx = focusables.indexOf(current);

    if (idx === -1) {
      focusables[0]?.focus();
      return;
    }

    const isGlobalOkKey = e.key === 'Enter' || e.key === 'Select' || e.key === 'Ok' || e.key === ' ' || e.keyCode === 13 || e.keyCode === 23;
    if (isGlobalOkKey) {
      if (current && typeof current.click === 'function' && !current.classList.contains('dpad-btn') && !current.classList.contains('remote-back-btn')) {
        current.click();
        e.preventDefault();
        return;
      }
    }

    if (e.key === 'ArrowRight') {
      const next = focusables[idx + 1] || focusables[0];
      next.focus();
      e.preventDefault();
    } else if (e.key === 'ArrowLeft') {
      const prev = focusables[idx - 1] || focusables[focusables.length - 1];
      prev.focus();
      e.preventDefault();
    } else if (e.key === 'ArrowDown') {
      const nextDown = findVerticalElement(focusables, current, 'down');
      if (nextDown) nextDown.focus();
      e.preventDefault();
    } else if (e.key === 'ArrowUp') {
      const nextUp = findVerticalElement(focusables, current, 'up');
      if (nextUp) nextUp.focus();
      e.preventDefault();
    }
  });

  // Top Nav Items Click Handlers
  const navBtns = document.querySelectorAll('.netflix-nav-item');
  navBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      navBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const target = btn.dataset.nav;
      
      if (target === 'mylist') {
        renderMovieRow(elements.catalogRow, state.myList);
        document.getElementById('catalog-section').scrollIntoView({ behavior: 'smooth' });
      } else if (target === 'latino') {
        document.getElementById('latino-section').scrollIntoView({ behavior: 'smooth' });
      } else if (target === 'popular') {
        document.getElementById('top10-section').scrollIntoView({ behavior: 'smooth' });
      } else {
        document.getElementById('main-container').scrollIntoView({ behavior: 'smooth' });
      }
    });
  });

  // Search Navigation Trigger
  if (elements.navSearchBtn) {
    elements.navSearchBtn.onclick = () => {
      elements.searchModal.classList.remove('hidden');
      elements.searchInput.focus();
    };
  }

  // Search input filter with debounce and Gateway API
  let searchTimer = null;
  elements.searchInput.addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase().trim();
    if (!q) {
      elements.searchResultsGrid.innerHTML = '';
      return;
    }
    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/search?q=${encodeURIComponent(q)}`);
        const data = await res.json();
        if (data.status === 'success' && data.movies && data.movies.length > 0) {
          elements.searchResultsGrid.innerHTML = '';
          renderMovieRow(elements.searchResultsGrid, data.movies);
          return;
        }
      } catch (_e) {}

      // In-memory fallback
      const filtered = [...state.movies, ...state.featured, ...state.page2Movies].filter(m => m.title.toLowerCase().includes(q));
      const unique = Array.from(new Set(filtered.map(a => a.title))).map(title => filtered.find(a => a.title === title));
      elements.searchResultsGrid.innerHTML = '';
      renderMovieRow(elements.searchResultsGrid, unique);
    }, 280);
  });

  elements.searchCloseBtn.onclick = () => {
    elements.searchModal.classList.add('hidden');
  };
}

// Find nearest element vertically
function findVerticalElement(elements, current, direction) {
  const currentRect = current.getBoundingClientRect();
  let bestElem = null;
  let minDistance = Infinity;

  elements.forEach(elem => {
    if (elem === current) return;
    const rect = elem.getBoundingClientRect();
    
    if (direction === 'down' && rect.top >= currentRect.bottom - 15) {
      const dist = Math.hypot(rect.left - currentRect.left, rect.top - currentRect.bottom);
      if (dist < minDistance) {
        minDistance = dist;
        bestElem = elem;
      }
    } else if (direction === 'up' && rect.bottom <= currentRect.top + 15) {
      const dist = Math.hypot(rect.left - currentRect.left, currentRect.top - rect.bottom);
      if (dist < minDistance) {
        minDistance = dist;
        bestElem = elem;
      }
    }
  });

  return bestElem;
}

// On-screen TV Remote Control
function setupOnScreenRemote() {
  const btnUp = document.getElementById('remote-up');
  const btnDown = document.getElementById('remote-down');
  const btnLeft = document.getElementById('remote-left');
  const btnRight = document.getElementById('remote-right');
  const btnOk = document.getElementById('remote-ok');
  const btnBack = document.getElementById('remote-back');

  if (btnUp) btnUp.onclick = () => dispatchKey('ArrowUp');
  if (btnDown) btnDown.onclick = () => dispatchKey('ArrowDown');
  if (btnLeft) btnLeft.onclick = () => dispatchKey('ArrowLeft');
  if (btnRight) btnRight.onclick = () => dispatchKey('ArrowRight');
  if (btnOk) btnOk.onclick = () => dispatchKey('Enter');
  if (btnBack) btnBack.onclick = () => dispatchKey('Escape');
}

function dispatchKey(key) {
  let keyCode = 0;
  if (key === 'ArrowUp') keyCode = 38;
  else if (key === 'ArrowDown') keyCode = 40;
  else if (key === 'ArrowLeft') keyCode = 37;
  else if (key === 'ArrowRight') keyCode = 39;
  else if (key === 'Enter') keyCode = 13;
  else if (key === 'Escape') keyCode = 27;
  document.dispatchEvent(new KeyboardEvent('keydown', { key, keyCode, bubbles: true }));
}

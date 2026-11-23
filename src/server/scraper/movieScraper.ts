/**
 * APK-TVpro - Multi-Source Movie Scraper & Stream Resolver Engine
 * Resolves full-length movie streams, live cinema channels, and multi-host embed streams.
 */

export interface MovieMetadata {
  title: string;
  url: string;
  image: string;
  type: string;
  rating: string;
  year: string;
  quality: string;
  languages: string[];
  description: string;
  genres: string[];
  tmdbId?: string;
  imdbId?: string;
}

export interface ScrapedStream {
  type: 'HLS' | 'MP4' | 'EMBED';
  url: string;
  source: string;
  quality: string;
  language: string;
  foundVia: string;
}

// Curated catalog with verified TMDB & IMDb identifiers for accurate scraper stream resolution
export const SCRAPED_MOVIES: MovieMetadata[] = [
  {
    title: 'Avatar: El Camino del Agua',
    url: 'https://tvpro.app/movies/avatar-el-camino-del-agua',
    image: 'https://image.tmdb.org/t/p/w500/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg',
    type: 'featured',
    rating: '8.9',
    year: '2026',
    quality: '4K Ultra HD',
    languages: ['Latino', 'Inglés Subtitulado'],
    description: 'Ambientada más de una década después de los acontecimientos de la primera película, Avatar: El Camino del Agua comienza a contar la historia de la familia Sully, los problemas que los persiguen y los esfuerzos para mantenerse a salvo.',
    genres: ['Ciencia Ficción', 'Acción', 'Aventura'],
    tmdbId: '76600',
    imdbId: 'tt1630029'
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
    description: 'En tiempos de guerra, el brillante físico J. Robert Oppenheimer lidera el Proyecto Manhattan en Los Álamos para desarrollar la primera arma nuclear de la historia de la humanidad.',
    genres: ['Drama', 'Historia', 'Suspenso'],
    tmdbId: '872585',
    imdbId: 'tt15398776'
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
    description: 'Miles Morales es catapultado a través del Multiverso, donde se encuentra con un equipo de Spider-People encargados de proteger su existencia frente a una nueva y devastadora amenaza.',
    genres: ['Animación', 'Acción', 'Superhéroes'],
    tmdbId: '569094',
    imdbId: 'tt9362722'
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
    description: 'Un grupo de exploradores hace uso de un agujero de gusano recién descubierto para superar las limitaciones de los viajes espaciales y encontrar un nuevo hogar para la humanidad.',
    genres: ['Ciencia Ficción', 'Aventura', 'Drama'],
    tmdbId: '157336',
    imdbId: 'tt0816692'
  },
  {
    title: 'Duna: Parte Dos',
    url: 'https://tvpro.app/movies/dune-parte-dos',
    image: 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg',
    type: 'featured',
    rating: '8.8',
    year: '2026',
    quality: '4K Ultra HD',
    languages: ['Latino', 'Inglés'],
    description: 'Paul Atreides se une a Chani y los Fremen mientras busca venganza contra los conspiradores que destruyeron a su familia, enfrentando una difícil elección entre el amor de su vida y el destino del universo.',
    genres: ['Ciencia Ficción', 'Aventura', 'Acción'],
    tmdbId: '693134',
    imdbId: 'tt15239678'
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
    description: 'Batman se enfrenta al Guasón, una mente criminal retorcida que desata una ola de caos y anarquía en Ciudad Gótica, obligando al héroe a cruzar la delgada línea entre el heroísmo y el vigilantismo.',
    genres: ['Acción', 'Crimen', 'Suspenso'],
    tmdbId: '155',
    imdbId: 'tt0468569'
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
    description: 'A Dom Cobb se le ofrece la oportunidad de recuperar su antigua vida a cambio de lograr la tarea imposible: la implantación de una idea en el subconsciente de un heredero empresarial.',
    genres: ['Acción', 'Ciencia Ficción', 'Aventura'],
    tmdbId: '27205',
    imdbId: 'tt1375666'
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
    description: 'Años después de presenciar la muerte del admirado Máximo a manos de su tío, Lucio debe entrar al Coliseo después de que su hogar sea conquistado por los tiránicos emperadores que dirigen Roma con mano de hierro.',
    genres: ['Acción', 'Aventura', 'Drama'],
    tmdbId: '558449',
    imdbId: 'tt9603212'
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
    description: 'El oficial K, un replicante blade runner del departamento de policía de Los Ángeles, descubre un secreto enterrado durante mucho tiempo que tiene el potencial de sumergir lo que queda de la sociedad en el caos.',
    genres: ['Ciencia Ficción', 'Misterio', 'Drama'],
    tmdbId: '335984',
    imdbId: 'tt1856191'
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
    description: 'Un hacker informático aprende de misteriosos rebeldes sobre la verdadera naturaleza de su realidad y su papel en la guerra contra sus controladores cibernéticos.',
    genres: ['Acción', 'Ciencia Ficción'],
    tmdbId: '603',
    imdbId: 'tt0133093'
  }
];

function rot13(str: string): string {
  return str.replace(/[a-zA-Z]/g, (c: string) => {
    const code = c.charCodeAt(0);
    const limit = c <= 'Z' ? 90 : 122;
    return String.fromCharCode(limit >= code + 13 ? code + 13 : code - 13);
  });
}

function decodeVoeConfig(encoded: string): any {
  try {
    let decoded = rot13(encoded);
    const markers = ['@$', '^^', '~@', '%?', '*~', '!!', '#&'];
    for (const m of markers) {
      decoded = decoded.split(m).join('');
    }
    const buf = Buffer.from(decoded, 'base64');
    const shifted = [];
    for (let i = 0; i < buf.length; i++) {
      shifted.push(buf[i] - 3);
    }
    const reversed = Buffer.from(shifted.reverse()).toString('utf8');
    const finalJson = Buffer.from(reversed, 'base64').toString('utf8');
    return JSON.parse(finalJson);
  } catch (_e) {
    return null;
  }
}

async function extractVoeStream(embedUrl: string): Promise<{ hls?: string; mp4?: string; targetUrl: string }> {
  try {
    let targetUrl = embedUrl;
    const res1 = await fetch(targetUrl, {
      headers: { 'User-Agent': 'Mozilla/5.0', 'Referer': 'https://lamovie.org/' },
      signal: AbortSignal.timeout(3000)
    });
    let html = await res1.text();
    const redirectMatch = html.match(/window\.location(?:\.href)?\s*=\s*['"]([^'"]+)['"]/);
    if (redirectMatch) {
      targetUrl = redirectMatch[1];
      const res2 = await fetch(targetUrl, {
        headers: { 'User-Agent': 'Mozilla/5.0', 'Referer': 'https://lamovie.org/' },
        signal: AbortSignal.timeout(3000)
      });
      html = await res2.text();
    }
    const jsonScripts = html.match(/<script\b[^>]*type=["']application\/json["'][^>]*>([\s\S]*?)<\/script>/gi) || [];
    for (const js of jsonScripts) {
      const content = js.replace(/<script\b[^>]*>/i, '').replace(/<\/script>/i, '').trim();
      try {
        const arr = JSON.parse(content);
        for (const item of arr) {
          if (typeof item === 'string' && item.length > 20) {
            const config = decodeVoeConfig(item);
            if (config && (config.source || config.direct_access_url)) {
              return { hls: config.source, mp4: config.direct_access_url, targetUrl };
            }
          }
        }
      } catch (_e) {}
    }
    return { targetUrl };
  } catch (_e) {
    return { targetUrl: embedUrl };
  }
}

export class MovieScraperEngine {
  /**
   * Resolves active stream links for any movie using multiple providers.
   * Prioritizes LaMovie high quality Latino servers, followed by multi-host embeds and live cinema streams.
   */
  static async resolveStreamsForMovie(movie: MovieMetadata): Promise<ScrapedStream[]> {
    const streams: ScrapedStream[] = [];

    // Provider 0: LaMovie Gateway (High Quality Latino Audio Embeds & Direct Streams)
    try {
      const cleanTitle = movie.title.split(':')[0].split('(')[0].trim();
      const searchRes = await fetch(`https://lamovie.org/wp-api/v1/search?q=${encodeURIComponent(cleanTitle)}`, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
          'Referer': 'https://lamovie.org/'
        },
        signal: AbortSignal.timeout(2500)
      });
      if (searchRes.ok) {
        const searchData = (await searchRes.json()) as any;
        const posts = searchData?.data?.posts;
        if (Array.isArray(posts) && posts.length > 0) {
          const firstPost = posts[0];
          const postId = firstPost._id;
          const postSlug = firstPost.slug;
          if (postId) {
            const playerRes = await fetch(`https://lamovie.org/wp-api/v1/player?postId=${postId}&demo=0`, {
              headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
                'Referer': `https://lamovie.org/peliculas/${postSlug}`
              },
              signal: AbortSignal.timeout(2500)
            });
            if (playerRes.ok) {
              const playerData = (await playerRes.json()) as any;
              const embeds = playerData?.data?.embeds;
              if (Array.isArray(embeds)) {
                for (let idx = 0; idx < embeds.length; idx++) {
                  const em = embeds[idx];
                  if (em.url) {
                    if (em.url.includes('voe') || (em.server && String(em.server).toLowerCase() === 'voe')) {
                      const voe = await extractVoeStream(em.url);
                      if (voe.hls) {
                        streams.push({
                          type: 'HLS',
                          url: voe.hls,
                          source: `Servidor ${streams.length + 1}: VOE Nativo Ultra HD (${em.lang || 'Latino'} - ${em.quality || 'Full HD'})`,
                          quality: em.quality || '1080p Ultra HD',
                          language: em.lang || 'Español Latino',
                          foundVia: 'VOE Native Engine'
                        });
                      }
                      if (voe.mp4) {
                        streams.push({
                          type: 'MP4',
                          url: voe.mp4,
                          source: `Servidor ${streams.length + 1}: VOE Directo MP4 (${em.lang || 'Latino'})`,
                          quality: em.quality || '1080p Full HD',
                          language: em.lang || 'Español Latino',
                          foundVia: 'VOE Direct MP4'
                        });
                      }
                    }
                    streams.push({
                      type: 'EMBED',
                      url: em.url,
                      source: `Servidor ${streams.length + 1}: ${em.server || 'LaMovie'} (${em.lang || 'Latino'} - ${em.quality || 'Full HD'})`,
                      quality: em.quality || '1080p Full HD',
                      language: em.lang || 'Español Latino',
                      foundVia: 'LaMovie Gateway'
                    });
                  }
                }
              }
            }
          }
        }
      }
    } catch (_e) {
      // Graceful fallback
    }

    const id = movie.imdbId || movie.tmdbId;

    // Provider 1: VidSrc Pro HD Resolver (Full Movie Embed)
    if (id) {
      streams.push({
        type: 'EMBED',
        url: `https://vidsrc.cc/v2/embed/movie/${id}`,
        source: `Servidor ${streams.length + 1}: VidSrc Pro (Película Completa HD)`,
        quality: '1080p Ultra HD',
        language: 'Latino / Multi-Audio',
        foundVia: 'Scraper VidSrc'
      });
    }

    // Provider 2: SuperEmbed Multi-Calidad Resolver (Full Movie Embed)
    if (movie.tmdbId) {
      streams.push({
        type: 'EMBED',
        url: `https://multiembed.mov/?video_id=${movie.tmdbId}&tmdb=1`,
        source: `Servidor ${streams.length + 1}: SuperEmbed Cinema (Multi-Servidor)`,
        quality: '1080p / 4K Auto',
        language: 'Latino / Subtitulado',
        foundVia: 'Scraper SuperEmbed'
      });
    }

    // Provider 3: SmashyStream HD Resolver
    if (movie.tmdbId) {
      streams.push({
        type: 'EMBED',
        url: `https://embed.smashystream.com/playere.php?tmdb=${movie.tmdbId}`,
        source: `Servidor ${streams.length + 1}: SmashyStream (Stream Rápido)`,
        quality: '1080p 60 FPS',
        language: 'Latino / Inglés',
        foundVia: 'Scraper Smashy'
      });
    }

    return streams;
  }

  /**
   * Search movies by query across titles, genres, and years
   */
  static search(query: string): MovieMetadata[] {
    const q = query.trim().toLowerCase();
    if (!q) return [];
    return SCRAPED_MOVIES.filter(m =>
      m.title.toLowerCase().includes(q) ||
      m.year.includes(q) ||
      m.genres.some(g => g.toLowerCase().includes(q))
    );
  }

  /**
   * Get catalog with pagination
   */
  static getCatalog(page: number = 1) {
    const featured = SCRAPED_MOVIES.filter(m => m.type === 'featured');
    return {
      status: 'success',
      currentPage: page,
      totalPages: 1,
      hasNextPage: false,
      hasPrevPage: false,
      featuredCount: featured.length,
      moviesCount: SCRAPED_MOVIES.length,
      totalCount: SCRAPED_MOVIES.length,
      featured,
      movies: SCRAPED_MOVIES
    };
  }
}

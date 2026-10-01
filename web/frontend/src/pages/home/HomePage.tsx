import { useState } from 'react'
import './HomePage.css'

interface Incidente {
  id: string
  categoria: string
  comuna: string
  tiempo: string
  reportadoPor: string
  estado: string
  descripcion: string
  mediaType: 'foto' | 'video'
  mediaCount?: string
  comentarios: { nombre: string; texto: string }[]
}

const incidentes: Incidente[] = [
  {
    id: 'INC-4021',
    categoria: 'Incendio forestal',
    comuna: 'Carahue',
    tiempo: 'hace 6 min',
    reportadoPor: 'Camila Paine',
    estado: 'NUEVO',
    descripcion: 'Columna de humo activa junto al cruce principal, aprox. 2 km del acceso a Carahue.',
    mediaType: 'foto',
    mediaCount: '1/2',
    comentarios: [
      { nombre: 'Matias Ordenes', texto: 'Brigada forestal en camino, ETA 15 min.' }
    ]
  },
  {
    id: 'INC-4018',
    categoria: 'Corte de servicio',
    comuna: 'Loncoche',
    tiempo: 'hace 40 min',
    reportadoPor: 'Bastian Huenul',
    estado: 'EN REVISION',
    descripcion: 'Corte de suministro electrico afecta a varias viviendas del sector rural.',
    mediaType: 'video',
    mediaCount: '0:18',
    comentarios: []
  },
  {
    id: 'INC-4015',
    categoria: 'Arbol caido',
    comuna: 'Padre Las Casas',
    tiempo: 'hace 2 h',
    reportadoPor: 'Elena Millao',
    estado: 'EN ATENCION',
    descripcion: 'Arbol de gran tamano bloquea completamente la via de acceso al sector.',
    mediaType: 'foto',
    mediaCount: undefined,
    comentarios: []
  }
]

const estadoClase: Record<string, string> = {
  'NUEVO': 'pill-nuevo',
  'EN REVISION': 'pill-revision',
  'EN ATENCION': 'pill-atencion',
  'RESUELTO': 'pill-resuelto',
  'CERRADO': 'pill-cerrado'
}

const mediaGradient: Record<string, string> = {
  'INC-4021': 'linear-gradient(135deg,#fdba74,#f97316 55%,#7c2d12)',
  'INC-4018': 'linear-gradient(135deg,#93c5fd,#3b82f6 55%,#1e3a8a)',
  'INC-4015': 'linear-gradient(135deg,#86efac,#16a34a 55%,#14532d)'
}

export function HomePage() {
  const [filtroActivo, setFiltroActivo] = useState('Todos')
  const [comentarios, setComentarios] = useState<Record<string, string>>({})

  const handleComentario = (id: string) => {
    const texto = comentarios[id]
    if (!texto?.trim()) return
    alert(`Comentario agregado en ${id}: ${texto}`)
    setComentarios(prev => ({ ...prev, [id]: '' }))
  }

  return (
    <div className="home-page">
      <div className="wm-head">
        <div>
          <h2>Inicio</h2>
          <p></p>
        </div>
        <button className="btn btn-primary btn-small">+ Nuevo reporte</button>
      </div>

      <div className="filters-row">
        <input type="text" placeholder="Buscar por categoria o comuna..." />
        <select>
          <option>Todos los estados</option>
          <option>Nuevo</option>
          <option>En revision</option>
          <option>Asignado</option>
          <option>En atencion</option>
          <option>Resuelto</option>
          <option>Cerrado</option>
        </select>
      </div>

      <div className="chip-row">
        {['Todos', 'Mis reportes', 'Nuevo', 'En atencion', 'Resuelto'].map(chip => (
          <div
            key={chip}
            className={`chip ${filtroActivo === chip ? 'on' : ''}`}
            onClick={() => setFiltroActivo(chip)}
          >
            {chip}
          </div>
        ))}
      </div>

      <div className="home-grid">
        <div className="home-list">
          {incidentes.map(inc => (
            <div key={inc.id} className="rep-card">
              <div className="rep-top">
                <div>
                  <div className="rep-cat">{inc.categoria}</div>
                  <div className="rep-meta">{inc.comuna} - {inc.tiempo} - reportado por {inc.reportadoPor}</div>
                </div>
                <div className={`pill ${estadoClase[inc.estado] || ''}`}>{inc.estado}</div>
              </div>

              <div className="rep-desc">{inc.descripcion}</div>

              <div className="rep-media" style={{ background: mediaGradient[inc.id] }}>
                {inc.mediaType === 'foto' ? (
                  <>
                    <span className="media-badge">Foto</span>
                    {inc.mediaCount && <span className="media-count">{inc.mediaCount}</span>}
                  </>
                ) : (
                  <>
                    <div className="media-play">
                      <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
                    </div>
                    <span className="media-badge">{inc.mediaCount}</span>
                  </>
                )}
              </div>

              <div className="pipeline">
                <div className="seg on red"></div>
                <div className={`seg ${inc.estado !== 'NUEVO' ? 'on orange' : ''}`}></div>
                <div className={`seg ${inc.estado === 'EN ATENCION' ? 'on blue' : ''}`}></div>
                <div className="seg"></div>
                <div className="seg"></div>
                <div className="seg"></div>
                <div className="seg"></div>
              </div>

              {inc.comentarios.length > 0 && (
                <div className="feed-comments">
                  {inc.comentarios.map((c, i) => (
                    <div key={i} className="comment-item">
                      <strong>{c.nombre}</strong>
                      <span>{c.texto}</span>
                    </div>
                  ))}
                </div>
              )}

              <div className="feed-comment-box">
                <input
                  type="text"
                  placeholder="Escribe un comentario..."
                  value={comentarios[inc.id] || ''}
                  onChange={(e) => setComentarios(prev => ({ ...prev, [inc.id]: e.target.value }))}
                />
                <button className="btn btn-secondary btn-small" onClick={() => handleComentario(inc.id)}>
                  Comentar
                </button>
              </div>
            </div>
          ))}
        </div>

        <div className="panel">
          <h4>MAPA TERRITORIAL</h4>
          <div className="home-map">
            <div className="pin red" style={{ top: '30%', left: '40%' }}>
              <div className="drop"></div>
            </div>
            <div className="pin blue" style={{ top: '25%', left: '70%' }}>
              <div className="drop"></div>
            </div>
            <div className="pin red" style={{ top: '55%', left: '30%' }}>
              <div className="drop"></div>
            </div>
            <div className="pin blue" style={{ top: '60%', left: '60%' }}>
              <div className="drop"></div>
            </div>
          </div>

          <div className="inc-list">
            <div className="inc-row">
              <div className="dot" style={{ background: 'var(--red)' }}></div>
              <div className="txt">
                <strong>Incendio forestal</strong>
                <span>Carahue - INC-4021</span>
              </div>
            </div>
            <div className="inc-row">
              <div className="dot" style={{ background: 'var(--orange)' }}></div>
              <div className="txt">
                <strong>Corte de servicio</strong>
                <span>Loncoche - INC-4018</span>
              </div>
            </div>
            <div className="inc-row">
              <div className="dot" style={{ background: 'var(--blue)' }}></div>
              <div className="txt">
                <strong>Arbol caido</strong>
                <span>Padre Las Casas - INC-4015</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
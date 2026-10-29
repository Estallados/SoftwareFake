export interface Resena {
  idResena: number;
  calificacion: number;
  comentario: string;
  estado: string;
  fecha: string;
  idUsuario: number;
  idReserva: number;
  idAlojamiento: number;
  nombreUsuario: string;
  tituloAlojamiento: string;
}

export interface ResumenResenas {
  idAlojamiento: number;
  promedio: number;
  cantidad: number;
  resenas: {
    contenido: Resena[];
  };
}

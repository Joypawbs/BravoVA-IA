package com.example.platform

/**
 * ¿Es un teléfono de gama baja?
 *
 * Para qué: mantener TODOS los efectos pero hacerlos más baratos donde hace falta
 * (desenfoque calculado a menor resolución, fondos animados que se actualizan menos veces por segundo).
 * Android: poca memoria, pocos núcleos o Android viejo. iPhone: menos de 4 GB o modo de bajo consumo.
 */
expect object DevicePerformance {
    val isLowEnd: Boolean
}

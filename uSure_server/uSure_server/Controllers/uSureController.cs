using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;

namespace uSure_server.Controllers
{
    [ApiController]
    [Route("[controller]")]
    public class uSureController : ControllerBase
    {
        private readonly ApplicationDbContext _context;
        private readonly ILogger<uSureController> _logger;

        public uSureController(ILogger<uSureController> logger, ApplicationDbContext context)
        {
            _logger = logger;
            _context = context;
        }

        [HttpPost("Login")]
        public async Task<IActionResult> Login(LoginRequest request)
        {
            // Lógica de autenticación para validar al usuario

            var user = await _context.Usuarios.FirstOrDefaultAsync(u => u.Nombre == request.Nombre && u.Password == request.Password);

            if (user == null)
            {
                return BadRequest("Nombre de usuario o contraseña incorrectos");
            }

            var tokenHandler = new JwtSecurityTokenHandler();
            var key = Encoding.ASCII.GetBytes("17471284wafawfawfwafawdwad1234141419248102adawd9999");
            var tokenDescriptor = new SecurityTokenDescriptor
            {
                Subject = new ClaimsIdentity(new Claim[]
                {
                    new Claim(ClaimTypes.Name, user.Nombre),
                    new Claim("ID", user.UID.ToString()) // Asignación del claim "ID"
                }),
                Expires = DateTime.UtcNow.AddDays(7), // Tiempo de expiración del token
                SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256Signature)
            };
            var token = tokenHandler.CreateToken(tokenDescriptor);
            var tokenString = tokenHandler.WriteToken(token);

            return Ok(new { Token = tokenString });
        }

        [HttpGet("UserList")]
        public async Task<IActionResult> Get()
        {
            var users = await _context.Usuarios.ToListAsync();

            var options = new JsonSerializerOptions
            {
                WriteIndented = true,
                ReferenceHandler = ReferenceHandler.Preserve
            };

            string jsonString = JsonSerializer.Serialize(users, options);

            return Ok(jsonString);
        }

        [HttpPost("Register")]
        public async Task<IActionResult> Register(RegisterRequest request)
        {
            var existingUser = await _context.Usuarios.FirstOrDefaultAsync(u => u.Nombre == request.Nombre || u.Email == request.Email);

            if (existingUser != null)
            {
                return BadRequest("Ya existe un usuario con el mismo nombre o correo electrónico");
            }

            var newUser = new Usuario
            {
                Nombre = request.Nombre,
                Email = request.Email,
                Password = request.Password,
                UID = Guid.NewGuid()
            };

            var basicGroup = await _context.Grupos.FirstOrDefaultAsync(g => g.Nombre == "Basico");

            if (basicGroup == null)
            {
                basicGroup = new Grupo
                {
                    Nombre = "Basico",
                    Codigo = Guid.NewGuid().ToString(),
                    Usuarios = new List<UsuarioGrupo>()
                };
                _context.Grupos.Add(basicGroup);
            }

            var usuarioGrupo = new UsuarioGrupo
            {
                Usuario = newUser,
                Grupo = basicGroup
            };

            _context.UsuarioGrupo.Add(usuarioGrupo);
            _context.Usuarios.Add(newUser);
            await _context.SaveChangesAsync();

            var options = new JsonSerializerOptions
            {
                WriteIndented = true,
                ReferenceHandler = ReferenceHandler.Preserve
            };

            string jsonString = JsonSerializer.Serialize(newUser, options);

            return Ok(jsonString);
        }

        [HttpPost("CreateGroup")]
        public async Task<IActionResult> CreateGroup(CreateGroupRequest request)
        {
            try
            {
                var newGroup = new Grupo
                {
                    Codigo = request.Codigo,
                    Nombre = request.Nombre,
                    Usuarios = new List<UsuarioGrupo>()
                };

                foreach (var userId in request.Usuarios)
                {
                    var usuarioGrupo = new UsuarioGrupo
                    {
                        UsuarioUID = userId,
                        Grupo = newGroup
                    };

                    newGroup.Usuarios.Add(usuarioGrupo);
                }

                _context.Grupos.Add(newGroup);
                await _context.SaveChangesAsync();

                var options = new JsonSerializerOptions
                {
                    WriteIndented = true,
                    ReferenceHandler = ReferenceHandler.Preserve
                };

                string jsonString = JsonSerializer.Serialize(newGroup, options);

                return Ok(jsonString);
            }
            catch (Exception e)
            {
                _logger.LogError(e, "Error al crear el grupo");
                return StatusCode(500, "Error interno del servidor al crear el grupo");
            }
        }

        [HttpGet("Groups")]
        public async Task<IActionResult> GetGroups()
        {
            var groups = await _context.Grupos.Include(g => g.Usuarios).ToListAsync();

            var options = new JsonSerializerOptions
            {
                WriteIndented = true,
                ReferenceHandler = ReferenceHandler.Preserve
            };

            string jsonString = JsonSerializer.Serialize(groups, options);

            return Ok(jsonString);
        }

        [HttpPost("JoinGroup")]
        public async Task<IActionResult> JoinGroup(JoinGroupRequest request)
        {
            var userUID = request.UsuarioUID;
            var groupCode = request.CodigoGrupo;

            var group = await _context.Grupos.FirstOrDefaultAsync(g => g.Codigo == groupCode);
            if (group == null)
            {
                return NotFound("El grupo con el código especificado no existe.");
            }

            var usuarioGrupo = new UsuarioGrupo
            {
                UsuarioUID = userUID,
                GrupoId = group.ID
            };

            _context.UsuarioGrupo.Add(usuarioGrupo);
            await _context.SaveChangesAsync();

            return Ok("Usuario añadido al grupo correctamente.");
        }
        [HttpGet("UserGroups")]
        [Authorize]
        public async Task<IActionResult> GetUserGroups()
        {
            try
            {
                var userIdClaim = User.FindFirst("ID");

                if (userIdClaim == null)
                {
                    return Unauthorized("El token no contiene un UID válido");
                }

                // Asegurar el nombre del claim y parsear su valor
                if (!Guid.TryParse(userIdClaim.Value, out Guid userUid))
                {
                    return Unauthorized("El token no contiene un UID válido");
                }

                var userGroups = await _context.UsuarioGrupo
                    .Where(ug => ug.UsuarioUID == userUid)
                    .Include(ug => ug.Grupo)
                    .Select(ug => ug.Grupo)
                    .ToListAsync();

                if (userGroups == null || userGroups.Count == 0)
                {
                    return NotFound("El usuario no pertenece a ningún grupo");
                }

                var options = new JsonSerializerOptions
                {
                    WriteIndented = true,
                    ReferenceHandler = ReferenceHandler.Preserve
                };

                string jsonString = JsonSerializer.Serialize(userGroups, options);

                return Ok(jsonString);
            }
            catch (Exception e)
            {
                _logger.LogError(e, "Error al obtener los grupos del usuario");
                return StatusCode(500, "Error interno del servidor al obtener los grupos del usuario");
            }
        }

        [HttpGet("Groups/{groupName}/Tables")]
        public async Task<IActionResult> GetTables(string groupName)
        {
            try
            {
                var userIdClaim = User.FindFirst("ID");

                if (userIdClaim == null || !Guid.TryParse(userIdClaim.Value, out Guid userUid))
                {
                    return Unauthorized("El token no contiene un UID válido");
                }

                var userGroups = await _context.UsuarioGrupo
                    .Where(ug => ug.UsuarioUID == userUid)
                    .Include(ug => ug.Grupo)
                    .Select(ug => ug.Grupo)
                    .ToListAsync();

                var group = userGroups.FirstOrDefault(g => g.Nombre == groupName);

                if (group == null)
                {
                    return NotFound("El usuario no pertenece a este grupo");
                }

                var categories = await _context.Categorias
                    .Where(c => c.IDGrupo == group.ID)
                    .Include(c => c.Productos)
                    .ToListAsync();

                var options = new JsonSerializerOptions
                {
                    WriteIndented = true,
                    ReferenceHandler = ReferenceHandler.Preserve
                };

                string jsonString = JsonSerializer.Serialize(categories, options);

                return Ok(jsonString);
            }
            catch (Exception e)
            {
                _logger.LogError(e, "Error al obtener las tablas del grupo");
                return StatusCode(500, "Error interno del servidor al obtener las tablas del grupo");
            }
        }
        [HttpPost("CreateTable")]
        public async Task<ActionResult<Categoria>> PostCategoria([FromBody] CategoriaCreateRequest request)
        {
            if (request == null || string.IsNullOrEmpty(request.Nombre) || request.IDGrupo <= 0)
            {
                return BadRequest("Invalid data.");
            }

            var categoria = new Categoria
            {
                Nombre = request.Nombre,
                IDGrupo = request.IDGrupo,
                Productos = new List<Producto>()
            };

            _context.Categorias.Add(categoria);
            await _context.SaveChangesAsync();

            return CreatedAtAction(nameof(GetCategoriaById), new { id = categoria.ID }, categoria);
        }

        [HttpGet("{id}")]
        public async Task<ActionResult<Categoria>> GetCategoriaById(int id)
        {
            var categoria = await _context.Categorias.FindAsync(id);

            if (categoria == null)
            {
                return NotFound();
            }

            return categoria;
        }

        [HttpPost("createProduct")]
        public async Task<ActionResult<Producto>> PostProducto(ProductoCreateRequest request)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            // Verifica si la categoría con la ID especificada existe
            if (request.IDCategoria != 0)
            {
                var categoria = await _context.Categorias.FindAsync(request.IDCategoria);
                if (categoria == null)
                {
                    // Si la categoría no existe, devuelve un error 404 (Not Found)
                    return NotFound("La categoría especificada no existe.");
                }
            }

            // Convierte GrupoProductos de GrupoProductoCreateRequest a ICollection<GrupoProducto>
            var grupoProductos = request.GrupoProductos.Select(gpr => new GrupoProducto
            {
                IDGrupo = gpr.IDGrupo,
                // Asigna otros valores si es necesario
            }).ToList();

            // Crea un nuevo producto y asigna los valores
            var producto = new Producto
            {
                Nombre = request.Nombre,
                Cantidad = request.Cantidad,
                IDCategoria = request.IDCategoria,
                GrupoProductos = grupoProductos
            };

            // Agrega el producto al contexto
            _context.Productos.Add(producto);

            // Guarda los cambios en la base de datos
            await _context.SaveChangesAsync();

            // Devuelve el producto creado
            return CreatedAtAction(nameof(GetProducto), new { id = producto.ID }, producto);
        }




        [HttpGet("Productos/{id}")]
        public async Task<ActionResult<Producto>> GetProducto(int id)
        {
            var producto = await _context.Productos.FindAsync(id);

            if (producto == null)
            {
                return NotFound();
            }

            return producto;
        }

        [HttpGet("Groups/{groupName}/Tables/{categoria}/ProductList")]
        public async Task<IActionResult> GetProductListForTables(string groupName, string categoria)
        {
            try
            {
                var userIdClaim = User.FindFirst("ID");

                if (userIdClaim == null || !Guid.TryParse(userIdClaim.Value, out Guid userUid))
                {
                    return Unauthorized("El token no contiene un UID válido");
                }

                var userGroups = await _context.UsuarioGrupo
                    .Where(ug => ug.UsuarioUID == userUid)
                    .Include(ug => ug.Grupo)
                    .Select(ug => ug.Grupo)
                    .ToListAsync();

                var group = userGroups.FirstOrDefault(g => g.Nombre == groupName);

                if (group == null)
                {
                    return NotFound("El usuario no pertenece a este grupo");
                }

                // Encuentra la categoría específica
                var categoriaObj = await _context.Categorias
                    .FirstOrDefaultAsync(c => c.Nombre == categoria && c.IDGrupo == group.ID);

                if (categoriaObj == null)
                {
                    return NotFound("La categoría especificada no existe en este grupo");
                }

                // Obtiene los productos asociados a la categoría
                var productList = await _context.Productos
                    .Where(p => p.IDCategoria == categoriaObj.ID)
                    .ToListAsync();

                // Devuelve la lista de productos
                return Ok(productList);
            }
            catch (Exception e)
            {
                _logger.LogError(e, "Error al obtener la lista de productos de la tabla");
                return StatusCode(500, "Error interno del servidor al obtener la lista de productos de la tabla");
            }
        }
        [HttpDelete("DeleteTable/{id}")]
        public async Task<IActionResult> DeleteCategoria(int id)
        {
            var categoria = await _context.Categorias.FindAsync(id);
            if (categoria == null)
            {
                return NotFound();
            }

            _context.Categorias.Remove(categoria);
            await _context.SaveChangesAsync();

            return NoContent();
        }

    }
}
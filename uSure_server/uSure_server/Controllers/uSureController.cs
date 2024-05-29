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



    }
}
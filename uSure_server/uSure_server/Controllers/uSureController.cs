using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using System.Text.Json;
using System.Text.Json.Serialization;

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
            var user = await _context.Usuarios.FirstOrDefaultAsync(u => u.Nombre == request.Nombre && u.Password == request.Password);

            if (user == null)
            {
                return BadRequest("Nombre de usuario o contraseña incorrectos");
            }

            // Configurar las opciones de serialización JSON lo uso para evitar el error de bucles
            var options = new JsonSerializerOptions
            {
                WriteIndented = true,
                ReferenceHandler = ReferenceHandler.Preserve
            };

            string jsonString = JsonSerializer.Serialize(user, options);

            return Ok(jsonString);
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

            // Crear un nuevo grupo "Basico" si no existe
            var basicGroup = await _context.Grupos.FirstOrDefaultAsync(g => g.Nombre == "Basico");
           
                basicGroup = new Grupo
                {
                    Nombre = "Basico",
                    Codigo = Guid.NewGuid().ToString(),
                    Usuarios = new List<UsuarioGrupo>()
                };
                _context.Grupos.Add(basicGroup);
            

     
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
    }
}

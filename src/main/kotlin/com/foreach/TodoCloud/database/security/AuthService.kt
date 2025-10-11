package com.foreach.TodoCloud.database.security

import com.foreach.TodoCloud.database.model.RefreshToken
import com.foreach.TodoCloud.database.model.User
import com.foreach.TodoCloud.database.repository.RefreshTokenRepository
import com.foreach.TodoCloud.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64


@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val tokensRepository: RefreshTokenRepository
) {

    data class TokenPair(
        val accessToken: String, val refreshToken: String
    )

    fun register(name: String, email: String, password: String) {
        require(name.isNotBlank()) { "Name is required" }
        require(email.isNotBlank()) { "Email is required" }
        require(password.isNotBlank()) { "Password is required" }

        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null) throw ResponseStatusException(
            HttpStatus.CONFLICT,
            "A user with that email already exists."
        )


        val hashedPassword = hashEncoder.encode(password)
        userRepository.save(User(name = name, email = email, hashedPassword = hashedPassword))
    }


    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(email) ?: throw BadCredentialsException("Invalid credentials.")

        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException("Invalid credentials.")
        }

        val newAccessToken = jwtService.generateAccessToken(user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken, refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
        }

        val hashed = hashToken(refreshToken)
        tokensRepository.findByUserIdAndHashedToken(user.id, hashed) ?: throw ResponseStatusException(
            HttpStatusCode.valueOf(401), "Refresh token not recognized (maybe used or expired?)"
        )

        tokensRepository.deleteByUserIdAndHashedToken(user.id, hashed)

        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken, refreshToken = newRefreshToken
        )
    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        tokensRepository.save(
            RefreshToken(
                userId = userId, expiresAt = expiresAt, hashedToken = hashed
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
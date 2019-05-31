package com.example.motiongame

import android.widget.ImageView

class GameObject(var xCrd : Float, var yCrd : Float, val linkedView : ImageView, val startOffset : Float, val baseSpeed : Float, val objectScore: Int)